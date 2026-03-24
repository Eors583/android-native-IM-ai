package com.aiim.android.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiim.android.domain.ai.OnDeviceQaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val qaEngine: OnDeviceQaEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()
    private var generatingJob: Job? = null

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun send() {
        val question = _uiState.value.input.trim()
        if (question.isEmpty() || _uiState.value.isGenerating) return

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            content = question,
            fromUser = true
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isGenerating = true,
                error = null,
                metricsText = null
            )
        }

        val aiMessageId = UUID.randomUUID().toString()
        _uiState.update { state ->
            state.copy(
                messages = state.messages + AiMessage(
                    id = aiMessageId,
                    content = "",
                    fromUser = false
                )
            )
        }

        val startTs = System.currentTimeMillis()
        var firstTokenTs: Long? = null

        generatingJob = viewModelScope.launch {
            try {
                val result = qaEngine.streamAnswer(question) { token ->
                    if (firstTokenTs == null) {
                        firstTokenTs = System.currentTimeMillis()
                    }
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == aiMessageId) {
                                    msg.copy(content = msg.content + token)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }
                val finalContent = result.text.ifBlank {
                    if (result.stopped) "已停止生成。" else "模型未返回内容，请换个问题试试。"
                }
                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        messages = state.messages.map { msg ->
                            if (msg.id == aiMessageId) msg.copy(content = finalContent) else msg
                        },
                        metricsText = buildMetricsText(
                            totalMs = result.elapsedMs,
                            firstTokenMs = firstTokenTs?.minus(startTs),
                            prefillMs = result.prefillMs,
                            decodeMs = result.decodeMs,
                            promptTokens = result.promptTokens,
                            outputTokens = result.outputTokens,
                            stopped = result.stopped
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        messages = state.messages.map { msg ->
                            if (msg.id == aiMessageId && msg.content.isBlank()) {
                                msg.copy(content = "生成失败")
                            } else {
                                msg
                            }
                        },
                        error = e.message ?: "AI生成失败"
                    )
                }
            }
        }
    }

    fun stopGenerating() {
        if (!_uiState.value.isGenerating) return
        qaEngine.requestStop()
    }

    private fun buildMetricsText(
        totalMs: Long,
        firstTokenMs: Long?,
        prefillMs: Long?,
        decodeMs: Long?,
        promptTokens: Long?,
        outputTokens: Long?,
        stopped: Boolean
    ): String {
        val parts = mutableListOf<String>()
        if (stopped) parts += "已停止"
        firstTokenMs?.let { parts += "首Token ${it}ms" }
        parts += "总耗时 ${totalMs}ms"
        prefillMs?.let { parts += "prefill ${it}ms" }
        decodeMs?.let { parts += "decode ${it}ms" }
        promptTokens?.let { parts += "输入Token $it" }
        outputTokens?.let { parts += "输出Token $it" }
        return parts.joinToString(" · ")
    }
}

data class AiChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val error: String? = null,
    val metricsText: String? = null
)

data class AiMessage(
    val id: String,
    val content: String,
    val fromUser: Boolean
)
