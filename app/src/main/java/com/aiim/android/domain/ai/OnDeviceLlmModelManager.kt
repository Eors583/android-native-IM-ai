package com.aiim.android.domain.ai

data class OnDeviceModelOption(
    val id: String,
    val displayName: String
)

data class ModelDownloadProgress(
    val fileName: String,
    val stepIndex: Int,
    val stepCount: Int,
    val bytesReceived: Long = 0L,
    val bytesTotal: Long? = null,
    val skipped: Boolean = false
)

interface OnDeviceLlmModelManager {
    fun availableModels(): List<OnDeviceModelOption>
    fun getSelectedModelId(): String

    /** 当前选中模型的必需文件是否已在本地就绪（不发起网络请求） */
    fun isActiveModelReady(): Boolean

    suspend fun downloadAndActivateModel(
        modelId: String,
        forceRedownload: Boolean,
        onProgress: (ModelDownloadProgress) -> Unit
    ): Result<Unit>
}
