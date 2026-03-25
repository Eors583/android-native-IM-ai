package com.aiim.android.ui.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiim.android.domain.ai.ModelDownloadProgress
import com.aiim.android.domain.ai.OnDeviceLlmModelManager
import com.aiim.android.domain.ai.OnDeviceModelOption
import com.aiim.android.domain.ai.OnDeviceQaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.aiim.chat.R
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val qaEngine: OnDeviceQaEngine,
    private val modelManager: OnDeviceLlmModelManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()
    private var generatingJob: Job? = null
    private var downloadJob: Job? = null

    init {
        refreshSelectedModelLabels()
        refreshModelAvailability()
    }

    private fun refreshSelectedModelLabels() {
        val id = modelManager.getSelectedModelId()
        val name = modelManager.availableModels().find { it.id == id }?.displayName ?: id
        _uiState.update {
            it.copy(
                selectedModelId = id,
                selectedModelDisplayName = name,
                availableModels = modelManager.availableModels()
            )
        }
    }

    private fun refreshModelAvailability() {
        _uiState.update { it.copy(isActiveModelReady = modelManager.isActiveModelReady()) }
    }

    fun openModelPicker() {
        val current = _uiState.value.selectedModelId.ifEmpty {
            modelManager.availableModels().firstOrNull()?.id.orEmpty()
        }
        _uiState.update {
            it.copy(
                modelPickerVisible = true,
                pickerSelectedModelId = current
            )
        }
    }

    fun dismissModelPicker() {
        _uiState.update { it.copy(modelPickerVisible = false) }
    }

    fun selectPickerModel(modelId: String) {
        _uiState.update { it.copy(pickerSelectedModelId = modelId) }
    }

    fun confirmModelPickerSelection() {
        val id = _uiState.value.pickerSelectedModelId
        if (id.isEmpty()) return
        val display = modelManager.availableModels().find { it.id == id }?.displayName ?: id
        _uiState.update {
            it.copy(
                modelPickerVisible = false,
                downloadConfirmVisible = true,
                pendingDownloadModelId = id,
                pendingDownloadModelDisplayName = display
            )
        }
    }

    fun dismissDownloadConfirm() {
        _uiState.update {
            it.copy(
                downloadConfirmVisible = false,
                pendingDownloadModelId = null,
                pendingDownloadModelDisplayName = ""
            )
        }
    }

    fun confirmModelDownload() {
        val modelId = _uiState.value.pendingDownloadModelId ?: return
        val displayName = _uiState.value.pendingDownloadModelDisplayName
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadConfirmVisible = false,
                    downloadProgressVisible = true,
                    downloadError = null,
                    downloadProgress = ModelDownloadProgressUi(
                        modelDisplayName = displayName,
                        fileLabel = "",
                        overallProgress = 0f,
                        stepLabel = "",
                        detail = ""
                    )
                )
            }
            val result = try {
                modelManager.downloadAndActivateModel(
                    modelId = modelId,
                    forceRedownload = false
                ) { p ->
                    _uiState.update { s ->
                        s.copy(downloadProgress = mapProgress(p, displayName))
                    }
                }
            } catch (e: CancellationException) {
                _uiState.update { s ->
                    s.copy(
                        downloadProgressVisible = false,
                        downloadProgress = null,
                        pendingDownloadModelId = null,
                        pendingDownloadModelDisplayName = ""
                    )
                }
                throw e
            }
            result.fold(
                onSuccess = {
                    val opt = modelManager.availableModels().find { it.id == modelId }
                    _uiState.update { s ->
                        s.copy(
                            downloadProgressVisible = false,
                            downloadProgress = null,
                            selectedModelId = modelId,
                            selectedModelDisplayName = opt?.displayName ?: modelId,
                            pendingDownloadModelId = null,
                            pendingDownloadModelDisplayName = "",
                            isActiveModelReady = modelManager.isActiveModelReady()
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { s ->
                        s.copy(
                            downloadProgressVisible = false,
                            downloadProgress = null,
                            downloadError = e.message ?: "下载失败",
                            pendingDownloadModelId = null,
                            pendingDownloadModelDisplayName = ""
                        )
                    }
                }
            )
        }
    }

    fun cancelModelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    fun dismissDownloadError() {
        _uiState.update { it.copy(downloadError = null) }
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun send() {
        val question = _uiState.value.input.trim()
        if (question.isEmpty() || _uiState.value.isGenerating) return
        if (!modelManager.isActiveModelReady()) {
            _uiState.update {
                it.copy(error = appContext.getString(R.string.ai_model_not_ready_hint))
            }
            return
        }

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

    private fun mapProgress(p: ModelDownloadProgress, modelDisplayName: String): ModelDownloadProgressUi {
        val fileFraction = when {
            p.skipped -> 1.0
            p.bytesTotal != null && p.bytesTotal > 0 ->
                p.bytesReceived.toDouble() / p.bytesTotal.toDouble()
            else -> if (p.bytesReceived > 0) 0.35 else 0.0
        }
        val overall = ((p.stepIndex - 1) + fileFraction.coerceIn(0.0, 1.0)) / p.stepCount.toDouble()
        val detail = when {
            p.skipped -> "已存在，跳过"
            p.bytesTotal != null && p.bytesTotal > 0 ->
                "${formatBytes(p.bytesReceived)} / ${formatBytes(p.bytesTotal)}"
            p.bytesReceived > 0 -> "已下载 ${formatBytes(p.bytesReceived)}"
            else -> "连接中…"
        }
        return ModelDownloadProgressUi(
            modelDisplayName = modelDisplayName,
            fileLabel = p.fileName,
            overallProgress = overall.coerceIn(0.0, 1.0).toFloat(),
            stepLabel = "${p.stepIndex}/${p.stepCount}",
            detail = detail
        )
    }

    private fun formatBytes(n: Long): String {
        if (n < 1024L) return "$n B"
        val kb = n / 1024.0
        if (kb < 1024.0) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
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
    val metricsText: String? = null,
    val availableModels: List<OnDeviceModelOption> = emptyList(),
    val selectedModelId: String = "",
    val selectedModelDisplayName: String = "",
    val modelPickerVisible: Boolean = false,
    val pickerSelectedModelId: String = "",
    val downloadConfirmVisible: Boolean = false,
    val pendingDownloadModelId: String? = null,
    val pendingDownloadModelDisplayName: String = "",
    val downloadProgressVisible: Boolean = false,
    val downloadProgress: ModelDownloadProgressUi? = null,
    val downloadError: String? = null,
    val isActiveModelReady: Boolean = false
)

data class ModelDownloadProgressUi(
    val modelDisplayName: String,
    val fileLabel: String,
    val overallProgress: Float,
    val stepLabel: String,
    val detail: String
)

data class AiMessage(
    val id: String,
    val content: String,
    val fromUser: Boolean
)
