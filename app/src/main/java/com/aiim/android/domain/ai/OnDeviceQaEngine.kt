package com.aiim.android.domain.ai

interface OnDeviceQaEngine {
    suspend fun streamAnswer(
        question: String,
        onToken: (String) -> Unit
    ): QaGenerationResult

    fun requestStop()
}

data class QaGenerationResult(
    val text: String,
    val stopped: Boolean,
    val elapsedMs: Long,
    val prefillMs: Long?,
    val decodeMs: Long?,
    val promptTokens: Long?,
    val outputTokens: Long?
)
