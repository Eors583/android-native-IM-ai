package com.aiim.android.data.ai

import android.content.Context
import com.alibaba.mnnllm.android.llm.LlmSession
import com.aiim.android.domain.ai.ModelDownloadProgress
import com.aiim.android.domain.ai.OnDeviceLlmModelManager
import com.aiim.android.domain.ai.OnDeviceModelOption
import com.aiim.android.domain.ai.OnDeviceQaEngine
import com.aiim.android.domain.ai.QaGenerationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class MnnOnDeviceQaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : OnDeviceQaEngine, OnDeviceLlmModelManager {
    private val activeSessionRef = AtomicReference<LlmSession?>(null)
    private val modelPrepareMutex = Mutex()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun availableModels(): List<OnDeviceModelOption> = AVAILABLE_MODELS

    override fun getSelectedModelId(): String =
        prefs.getString(KEY_SELECTED_MODEL_ID, DEFAULT_MODEL_ID) ?: DEFAULT_MODEL_ID

    override fun isActiveModelReady(): Boolean = isModelReady(getSelectedModelId())

    override suspend fun downloadAndActivateModel(
        modelId: String,
        forceRedownload: Boolean,
        onProgress: (ModelDownloadProgress) -> Unit
    ): Result<Unit> {
        return try {
            modelPrepareMutex.withLock {
                withContext(Dispatchers.IO) {
                    syncModelFromOss(modelId, forceRedownload, onProgress)
                }
                prefs.edit().putString(KEY_SELECTED_MODEL_ID, modelId).apply()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun streamAnswer(
        question: String,
        onToken: (String) -> Unit
    ): QaGenerationResult = withContext(Dispatchers.Default) {
        val q = question.trim()
        if (q.isEmpty()) {
            return@withContext QaGenerationResult("", false, 0L, null, null, null, null)
        }

        modelPrepareMutex.withLock {
            if (!isModelReady(getSelectedModelId())) {
                throw IllegalStateException(MODEL_NOT_READY_USER_HINT)
            }
            generateByMnn(q, onToken)
        }
    }

    override fun requestStop() {
        activeSessionRef.get()?.requestStop()
    }

    private fun generateByMnn(
        question: String,
        onToken: (String) -> Unit
    ): QaGenerationResult {
        val configFile = File(modelStorageDir(getSelectedModelId()), "config.json")
        if (!configFile.exists()) {
            throw IllegalStateException("MNN配置缺失：未找到 config.json。")
        }
        val mergedConfigJson = loadMergedConfigJson()
        val runtimeConfigJson = buildRuntimeConfigJson()
        val startTs = System.currentTimeMillis()
        val session = LlmSession(
            configPath = configFile.absolutePath,
            mergedConfigJson = mergedConfigJson,
            runtimeConfigJson = runtimeConfigJson
        )
        activeSessionRef.set(session)
        return try {
            session.load()
            val result = session.generateStream(question, keepHistory = false, onToken = onToken)
            QaGenerationResult(
                text = result.text,
                stopped = result.stopped,
                elapsedMs = System.currentTimeMillis() - startTs,
                prefillMs = result.prefillUs?.div(1000L),
                decodeMs = result.decodeUs?.div(1000L),
                promptTokens = result.promptLen,
                outputTokens = result.decodeLen
            )
        } catch (e: Exception) {
            Timber.e(e, "MNN inference failed")
            throw IllegalStateException("MNN推理失败：${e.message ?: "未知错误"}", e)
        } finally {
            session.release()
            activeSessionRef.compareAndSet(session, null)
        }
    }

    private fun isModelReady(modelId: String): Boolean {
        val dir = modelStorageDir(modelId)
        val config = File(dir, "config.json")
        if (!config.isFile || config.length() == 0L) return false
        val names = readRuntimeModelFileNames(dir)
        val modelFile = File(dir, names.llmModel)
        val weightFile = File(dir, names.llmWeight)
        val tokenizerFile = File(dir, TOKENIZER_FILE_NAME)
        return modelFile.isFile && modelFile.length() > 0L &&
            weightFile.isFile && weightFile.length() > 0L &&
            tokenizerFile.isFile && tokenizerFile.length() > 0L
    }

    private suspend fun syncModelFromOss(
        modelId: String,
        forceRedownload: Boolean,
        onProgress: (ModelDownloadProgress) -> Unit
    ) {
        val modelDir = modelStorageDir(modelId)
        if (forceRedownload && modelDir.exists()) {
            modelDir.listFiles()?.forEach { it.delete() }
        }
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        val base = ossModelObjectBaseUrl(modelId)
        val stepCount = 1 + 2 + 1 + OPTIONAL_MODEL_FILES.size
        var stepIndex = 0

        fun report(
            fileName: String,
            received: Long = 0L,
            total: Long? = null,
            skipped: Boolean = false
        ) {
            onProgress(
                ModelDownloadProgress(
                    fileName = fileName,
                    stepIndex = stepIndex,
                    stepCount = stepCount,
                    bytesReceived = received,
                    bytesTotal = total,
                    skipped = skipped
                )
            )
        }

        stepIndex = 1
        val configFile = File(modelDir, "config.json")
        if (!forceRedownload && configFile.isFile && configFile.length() > 0L) {
            report("config.json", skipped = true)
        } else {
            downloadHttpFileWithProgress(
                "$base/config.json",
                configFile,
                required = true,
                onProgress = { r, t -> report("config.json", r, t) }
            )
        }

        val names = readRuntimeModelFileNames(modelDir)

        stepIndex = 2
        val llmFile = File(modelDir, names.llmModel)
        if (!forceRedownload && llmFile.isFile && llmFile.length() > 0L) {
            report(names.llmModel, skipped = true)
        } else {
            downloadHttpFileWithProgress(
                "$base/${names.llmModel}",
                llmFile,
                required = true,
                onProgress = { r, t -> report(names.llmModel, r, t) }
            )
        }

        stepIndex = 3
        val weightFile = File(modelDir, names.llmWeight)
        if (!forceRedownload && weightFile.isFile && weightFile.length() > 0L) {
            report(names.llmWeight, skipped = true)
        } else {
            downloadHttpFileWithProgress(
                "$base/${names.llmWeight}",
                weightFile,
                required = true,
                onProgress = { r, t -> report(names.llmWeight, r, t) }
            )
        }

        stepIndex = 4
        val tokenizerDest = File(modelDir, TOKENIZER_FILE_NAME)
        if (!forceRedownload && tokenizerDest.isFile && tokenizerDest.length() > 0L) {
            report(TOKENIZER_FILE_NAME, skipped = true)
        } else {
            downloadHttpFileWithProgress(
                "$base/$TOKENIZER_FILE_NAME",
                tokenizerDest,
                required = true,
                onProgress = { r, t -> report(TOKENIZER_FILE_NAME, r, t) }
            )
        }

        for ((i, name) in OPTIONAL_MODEL_FILES.withIndex()) {
            stepIndex = 5 + i
            val f = File(modelDir, name)
            if (!forceRedownload && f.isFile && f.length() > 0L) {
                report(name, skipped = true)
                continue
            }
            val ok = downloadHttpFileWithProgress(
                "$base/$name",
                f,
                required = false,
                onProgress = { r, t -> report(name, r, t) }
            )
            if (!ok) {
                report(name, skipped = true)
            }
        }
    }

    private fun modelStorageDir(modelId: String): File =
        File(context.filesDir, "$MODEL_ROOT/$modelId")

    private fun ossModelObjectBaseUrl(modelId: String): String {
        val root = OSS_MODEL_BASE.trimEnd('/')
        val id = modelId.trim('/')
        return "$root/$id"
    }

    private fun readRuntimeModelFileNames(modelDir: File): RuntimeModelNames {
        val configFile = File(modelDir, "config.json")
        if (!configFile.isFile) {
            return RuntimeModelNames(DEFAULT_LLM_MODEL, DEFAULT_LLM_WEIGHT)
        }
        return runCatching {
            val obj = JSONObject(configFile.readText())
            val m = obj.optString("llm_model", DEFAULT_LLM_MODEL).trim()
            val w = obj.optString("llm_weight", DEFAULT_LLM_WEIGHT).trim()
            RuntimeModelNames(
                llmModel = m.ifEmpty { DEFAULT_LLM_MODEL },
                llmWeight = w.ifEmpty { DEFAULT_LLM_WEIGHT }
            )
        }.getOrElse {
            Timber.w(it, "parse config.json failed, using default llm file names")
            RuntimeModelNames(DEFAULT_LLM_MODEL, DEFAULT_LLM_WEIGHT)
        }
    }

    private data class RuntimeModelNames(val llmModel: String, val llmWeight: String)

    /**
     * @return false if optional file was missing (404)
     */
    private suspend fun downloadHttpFileWithProgress(
        urlString: String,
        dest: File,
        required: Boolean,
        onProgress: (received: Long, total: Long?) -> Unit
    ): Boolean {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, "${dest.name}.part")
        if (tmp.exists()) tmp.delete()

        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        return try {
            conn.connect()
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND && !required) {
                return false
            }
            if (code != HttpURLConnection.HTTP_OK) {
                if (required) {
                    throw IllegalStateException(
                        "模型下载失败（HTTP $code），请检查网络或 OSS 地址是否可访问。"
                    )
                }
                Timber.w("HTTP $code for $urlString")
                return false
            }
            val contentLength = conn.contentLengthLong.takeIf { it > 0L }
            var received = 0L
            var lastReport = 0L
            conn.inputStream.use { raw ->
                BufferedInputStream(raw).use { input ->
                    FileOutputStream(tmp).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            received += n
                            val chunkDone = contentLength != null && received >= contentLength
                            if (received - lastReport >= PROGRESS_REPORT_INTERVAL_BYTES || chunkDone) {
                                onProgress(received, contentLength)
                                lastReport = received
                            }
                        }
                    }
                }
            }
            onProgress(received, contentLength)
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            tmp.delete()
            if (required) {
                throw IllegalStateException(networkErrorHint(e), e)
            }
            Timber.w(e, "optional model file download failed: %s", urlString)
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun networkErrorHint(e: Throwable): String = when (e) {
        is UnknownHostException -> "无法连接模型服务器，请检查网络或 DNS 后重试。"
        is SocketTimeoutException -> "下载超时，请稍后重试或检查网络。"
        is ConnectException -> "无法建立连接，请检查网络。"
        is SSLException -> "安全连接失败，请检查网络或系统时间。"
        is IOException -> "网络异常：${e.message ?: "请检查网络后重试"}"
        else -> e.message ?: "未知错误"
    }

    private fun loadMergedConfigJson(): String {
        val dir = modelStorageDir(getSelectedModelId())
        val primary = File(dir, "llm_config.json")
        val fallback = File(dir, "configuration.json")
        val source = when {
            primary.exists() -> primary
            fallback.exists() -> fallback
            else -> return "{}"
        }
        return runCatching {
            val raw = source.readText()
            val obj = JSONObject(raw)
            sanitizeMergedConfig(obj)
            obj.toString()
        }.getOrElse {
            Timber.w(it, "failed to read merged config: %s", source.absolutePath)
            "{}"
        }
    }

    private fun sanitizeMergedConfig(obj: JSONObject) {
        val stringKeys = listOf(
            "model_type",
            "attention_mask",
            "attention_type",
            "tokenizer",
            "llm_model",
            "llm_weight"
        )
        for (key in stringKeys) {
            if (obj.has(key) && obj.isNull(key)) {
                obj.put(key, "")
            }
        }
        if (obj.has("jinja") && obj.get("jinja") is JSONObject) {
            val jinja = obj.getJSONObject("jinja")
            if (jinja.has("eos") && jinja.isNull("eos")) {
                jinja.put("eos", "")
            }
            if (jinja.has("chat_template") && jinja.isNull("chat_template")) {
                jinja.put("chat_template", "")
            }
        }
        obj.put("is_visual", false)
    }

    private fun buildRuntimeConfigJson(): String {
        return JSONObject().apply {
            put("is_r1", false)
            put("mmap_dir", "")
            put("keep_history", false)
        }.toString()
    }

    private companion object {
        const val PREFS_NAME = "aiim_ondevice_llm"
        const val KEY_SELECTED_MODEL_ID = "selected_model_id"
        const val OSS_MODEL_BASE = "https://oss-mnn.obs.cn-south-1.myhuaweicloud.com/mnn"
        const val DEFAULT_MODEL_ID = "qwen3.5"
        val AVAILABLE_MODELS = listOf(
            OnDeviceModelOption(id = "qwen3.5", displayName = "Qwen 3.5")
        )
        const val MODEL_ROOT = "mnn"
        const val DEFAULT_LLM_MODEL = "llm.mnn"
        const val DEFAULT_LLM_WEIGHT = "llm.mnn.weight"
        const val TOKENIZER_FILE_NAME = "tokenizer.txt"
        val OPTIONAL_MODEL_FILES = listOf(
            "llm_config.json",
            "configuration.json",
            "llm.mnn.json",
            "visual.mnn",
            "visual.mnn.weight"
        )
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 600_000
        const val PROGRESS_REPORT_INTERVAL_BYTES = 256 * 1024

        /** 与 UI 文案保持一致，便于异常路径提示 */
        const val MODEL_NOT_READY_USER_HINT =
            "当前没有可用的本地模型，请先在右上角「切换模型」中下载模型。"
    }
}
