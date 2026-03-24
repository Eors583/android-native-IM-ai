package com.aiim.android.data.ai

import android.content.Context
import com.alibaba.mnnllm.android.llm.LlmSession
import com.aiim.android.domain.ai.OnDeviceQaEngine
import com.aiim.android.domain.ai.QaGenerationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnnOnDeviceQaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : OnDeviceQaEngine {
    private val activeSessionRef = AtomicReference<LlmSession?>(null)

    override suspend fun streamAnswer(
        question: String,
        onToken: (String) -> Unit
    ): QaGenerationResult = withContext(Dispatchers.Default) {
        val q = question.trim()
        if (q.isEmpty()) {
            return@withContext QaGenerationResult("", false, 0L, null, null, null, null)
        }

        ensureModelFilesReady()
        val modelDir = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        val weightFile = File(modelDir, MODEL_WEIGHT_FILE_NAME)
        val tokenizerFile = File(modelDir, TOKENIZER_FILE_NAME)
        if (!modelFile.exists() || !weightFile.exists()) {
            throw IllegalStateException("MNN模型未就绪：缺少 llm.mnn 或 llm.mnn.weight。")
        }
        if (!tokenizerFile.exists()) {
            throw IllegalStateException("MNN模型未就绪：缺少 tokenizer.txt。")
        }

        generateByMnn(q, onToken)
    }

    override fun requestStop() {
        activeSessionRef.get()?.requestStop()
    }

    private fun generateByMnn(
        question: String,
        onToken: (String) -> Unit
    ): QaGenerationResult {
        val configFile = File(context.filesDir, "$MODEL_DIR/config.json")
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

    private fun ensureModelFilesReady() {
        val modelDir = File(context.filesDir, MODEL_DIR)
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        copyAssetIfAbsent("$MODEL_DIR/$MODEL_FILE_NAME", File(modelDir, MODEL_FILE_NAME))
        copyAssetIfAbsent("$MODEL_DIR/$MODEL_WEIGHT_FILE_NAME", File(modelDir, MODEL_WEIGHT_FILE_NAME))
        copyAssetIfAbsent("$MODEL_DIR/$TOKENIZER_FILE_NAME", File(modelDir, TOKENIZER_FILE_NAME))
        copyAssetIfAbsent("$MODEL_DIR/config.json", File(modelDir, "config.json"))
        copyAssetIfAbsent("$MODEL_DIR/llm_config.json", File(modelDir, "llm_config.json"))
        copyAssetIfAbsent("$MODEL_DIR/llm.mnn.json", File(modelDir, "llm.mnn.json"))
        copyAssetIfAbsent("$MODEL_DIR/visual.mnn", File(modelDir, "visual.mnn"))
        copyAssetIfAbsent("$MODEL_DIR/visual.mnn.weight", File(modelDir, "visual.mnn.weight"))
    }

    private fun copyAssetIfAbsent(assetPath: String, outFile: File) {
        if (outFile.exists()) return
        runCatching {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        }.onFailure {
            Timber.w(it, "asset not found or copy failed: %s", assetPath)
        }
    }

    private fun loadMergedConfigJson(): String {
        val primary = File(context.filesDir, "$MODEL_DIR/llm_config.json")
        val fallback = File(context.filesDir, "$MODEL_DIR/configuration.json")
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
        // Some exported configs contain null for string fields and native parser throws.
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
        // This screen is text-only chat. For some exported multimodal configs,
        // forcing text mode avoids visual module load failures.
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
        const val MODEL_DIR = "mnn"
        const val MODEL_FILE_NAME = "llm.mnn"
        const val MODEL_WEIGHT_FILE_NAME = "llm.mnn.weight"
        const val TOKENIZER_FILE_NAME = "tokenizer.txt"
    }
}
