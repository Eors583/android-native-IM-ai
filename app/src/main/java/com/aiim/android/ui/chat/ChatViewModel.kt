package com.aiim.android.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiim.android.core.utils.NetworkUtils
import com.aiim.android.data.local.prefs.UserProfileLocalDataSource
import com.aiim.android.domain.model.ConnectionState
import com.aiim.android.domain.model.ChatRoomSummary
import com.aiim.android.domain.model.Message
import com.aiim.android.domain.usecase.ConnectToServerUseCase
import com.aiim.android.domain.usecase.DisconnectUseCase
import com.aiim.android.domain.usecase.GetConnectionStateUseCase
import com.aiim.android.domain.usecase.GetMessagesUseCase
import com.aiim.android.domain.usecase.SendMessageUseCase
import com.aiim.android.domain.usecase.SetNicknameUseCase
import com.aiim.android.domain.usecase.StartServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 聊天室ViewModel
 * 管理UI状态和业务逻辑
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userProfileLocalDataSource: UserProfileLocalDataSource,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val startServerUseCase: StartServerUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    private val getConnectionStateUseCase: GetConnectionStateUseCase,
    private val setNicknameUseCase: SetNicknameUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // 输入状态
    private val _inputState = MutableStateFlow(InputState())
    val inputState: StateFlow<InputState> = _inputState.asStateFlow()

    /** 连接页 / 聊天室 */
    private val _mainScreen = MutableStateFlow(MainScreen.Connection)
    val mainScreen: StateFlow<MainScreen> = _mainScreen.asStateFlow()

    init {
        val cachedUsername = userProfileLocalDataSource.load().username.trim()
        _inputState.update {
            it.copy(
                localHostIp = NetworkUtils.getLocalIpAddress(appContext),
                nickname = cachedUsername
            )
        }

        // 监听消息变化
        viewModelScope.launch {
            getMessagesUseCase().collectLatest { messages ->
                _uiState.update { it.copy(messages = messages.reversed()) }
            }
        }

        viewModelScope.launch {
            getMessagesUseCase.getChatRooms().collectLatest { rooms ->
                _uiState.update { it.copy(chatRooms = rooms) }
            }
        }

        // 监听连接状态变化（失败原因会通过 errorMessage 弹出 Snackbar）
        viewModelScope.launch {
            getConnectionStateUseCase().collectLatest { connectionState ->
                if (connectionState is ConnectionState.Connected) {
                    val nick = _inputState.value.nickname.trim()
                    if (nick.isNotEmpty()) {
                        setNicknameUseCase(nick)
                    }
                }
                if (connectionState is ConnectionState.Disconnected || connectionState is ConnectionState.Failed) {
                    _mainScreen.value = MainScreen.Connection
                }
                _uiState.update { state ->
                    state.copy(
                        connectionState = connectionState,
                        errorMessage = if (connectionState is ConnectionState.Failed) {
                            connectionState.error
                        } else {
                            state.errorMessage
                        }
                    )
                }
            }
        }
    }

    fun openChatRoom() {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        val nick = _inputState.value.nickname.trim()
        if (nick.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "请先在我的页面填写用户名") }
            return
        }
        val peerIp = _inputState.value.serverIp.trim().ifBlank { _inputState.value.localHostIp.trim() }
        viewModelScope.launch {
            getMessagesUseCase.createChatRoom(peerIp = peerIp)
            setNicknameUseCase(nick)
            _mainScreen.value = MainScreen.ChatRoom
        }
    }

    fun openHistoryChatRoom(roomId: String) {
        getMessagesUseCase.setActiveChatRoom(roomId)
        val nick = _inputState.value.nickname.trim()
        if (nick.isNotEmpty()) {
            setNicknameUseCase(nick)
        }
        _mainScreen.value = MainScreen.ChatRoom
    }

    fun backToConnection() {
        _mainScreen.value = MainScreen.Connection
    }

    /** 若界面显示的「本机 IP」不对，可手动刷新（应与系统设置里 WLAN 的 IPv4 一致） */
    fun refreshLocalIp() {
        _inputState.update {
            it.copy(localHostIp = NetworkUtils.getLocalIpAddress(appContext))
        }
    }

    /**
     * 发送消息（读 ViewModel 里暂存的 [InputState.messageInput]）
     */
    fun sendMessage() {
        val content = _inputState.value.messageInput.trim()
        val nickname = _inputState.value.nickname.trim()
        if (content.isEmpty() || nickname.isEmpty()) return
        sendMessageWithContent(_inputState.value.messageInput) { }
    }

    /**
     * 发送指定正文（聊天室用 Compose 的 TextFieldValue 持有时调用，避免中文 IME 组字被 StateFlow 打断）
     *
     * @param onFinished null 表示成功；非 null 为失败异常
     */
    fun sendMessageWithContent(
        rawContent: String,
        onFinished: (Throwable?) -> Unit = {}
    ) {
        val content = rawContent.trim()
        val nickname = _inputState.value.nickname.trim()
        if (content.isEmpty() || nickname.isEmpty()) {
            onFinished(IllegalStateException("内容或昵称为空"))
            return
        }
        viewModelScope.launch {
            try {
                sendMessageUseCase(content, nickname)
                _inputState.update { it.copy(messageInput = "") }
                onFinished(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "发送失败: ${e.message}") }
                onFinished(e)
            }
        }
    }

    /**
     * 更新消息输入
     */
    fun updateMessageInput(input: String) {
        _inputState.update { it.copy(messageInput = input) }
    }

    /**
     * 更新昵称输入
     */
    fun updateNicknameInput(input: String) {
        _inputState.update { it.copy(nickname = input) }
        if (_uiState.value.connectionState is ConnectionState.Connected && input.trim().isNotEmpty()) {
            setNicknameUseCase(input.trim())
        }
    }

    fun syncNicknameFromProfile(username: String) {
        updateNicknameInput(username.trim())
    }

    /**
     * 更新服务器IP输入
     */
    fun updateServerIpInput(input: String) {
        _inputState.update { it.copy(serverIp = input) }
    }

    /**
     * 连接到服务器
     */
    fun connectToServer() {
        _uiState.update { it.copy(errorMessage = null) }
        val serverIp = _inputState.value.serverIp.trim()
        if (serverIp.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "请输入对方手机的 IP 地址") }
            return
        }
        if (!NetworkUtils.isValidIpAddress(serverIp)) {
            _uiState.update {
                it.copy(errorMessage = "IP 格式不正确，请填类似 192.168.1.5 的四段数字")
            }
            return
        }

        viewModelScope.launch {
            try {
                connectToServerUseCase(serverIp)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "连接失败: ${e.message}") }
            }
        }
    }

    /**
     * 启动服务器
     */
    fun startServer() {
        _uiState.update { it.copy(errorMessage = null) }
        viewModelScope.launch {
            try {
                startServerUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "启动服务器失败: ${e.message}") }
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                disconnectUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "断开连接失败: ${e.message}") }
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 清除所有消息
     */
    fun clearAllMessages() {
        // TODO: 实现清除消息功能
        _uiState.update { it.copy(messages = emptyList()) }
    }
}

/**
 * UI状态数据类
 */
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val chatRooms: List<ChatRoomSummary> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

enum class MainScreen {
    Connection,
    ChatRoom
}

/**
 * 输入状态数据类
 */
data class InputState(
    val messageInput: String = "",
    val nickname: String = "",
    /** 要连接的对端 IP（填对方的手机 IP） */
    val serverIp: String = "",
    /** 本机在局域网中的 IP，供对方连接时使用 */
    val localHostIp: String = ""
)