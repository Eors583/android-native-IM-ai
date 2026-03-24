package com.alibaba.mnnllm.android.llm

@Suppress("UNCHECKED_CAST")
class LlmSession(
    private val configPath: String,
    private val mergedConfigJson: String,
    private val runtimeConfigJson: String
) {
    private var nativePtr: Long = 0L
    @Volatile
    private var stopRequested: Boolean = false

    fun load() {
        if (nativePtr != 0L) return
        nativePtr = initNative(
            configPath,
            null,
            mergedConfigJson,
            runtimeConfigJson
        )
        check(nativePtr != 0L) { "MNN native init failed" }
    }

    fun generateStream(
        prompt: String,
        keepHistory: Boolean = false,
        onToken: (String) -> Unit
    ): LlmGenerateResult {
        check(nativePtr != 0L) { "LlmSession not loaded" }
        stopRequested = false
        val builder = StringBuilder()
        val resultMap = submitNative(
            nativePtr,
            prompt,
            keepHistory,
            GenerateProgressListener { progress ->
                if (progress != null) {
                    builder.append(progress)
                    onToken(progress)
                }
                stopRequested
            }
        )
        return LlmGenerateResult(
            text = builder.toString(),
            stopped = stopRequested,
            promptLen = resultMap["prompt_len"]?.toLongCompat(),
            decodeLen = resultMap["decode_len"]?.toLongCompat(),
            prefillUs = resultMap["prefill_time"]?.toLongCompat(),
            decodeUs = resultMap["decode_time"]?.toLongCompat()
        )
    }

    fun release() {
        if (nativePtr == 0L) return
        releaseNative(nativePtr)
        nativePtr = 0L
    }

    fun requestStop() {
        stopRequested = true
    }

    private external fun initNative(
        configPath: String?,
        history: List<String>?,
        mergedConfigStr: String?,
        configJsonStr: String?
    ): Long

    private external fun submitNative(
        instanceId: Long,
        input: String,
        keepHistory: Boolean,
        listener: GenerateProgressListener
    ): HashMap<String, Any>

    private external fun releaseNative(instanceId: Long)

    companion object {
        init {
            System.loadLibrary("c++_shared")
            System.loadLibrary("MNN")
            System.loadLibrary("mnnllmapp")
        }
    }
}

data class LlmGenerateResult(
    val text: String,
    val stopped: Boolean,
    val promptLen: Long?,
    val decodeLen: Long?,
    val prefillUs: Long?,
    val decodeUs: Long?
)

private fun Any.toLongCompat(): Long? {
    return when (this) {
        is Long -> this
        is Int -> this.toLong()
        is Number -> this.toLong()
        is String -> this.toLongOrNull()
        else -> null
    }
}
