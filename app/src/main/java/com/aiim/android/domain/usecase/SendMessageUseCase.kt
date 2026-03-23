package com.aiim.android.domain.usecase

import com.aiim.android.domain.model.Message
import com.aiim.android.domain.model.ChatRoomSummary
import com.aiim.android.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 发送消息用例
 * 业务逻辑层，协调发送消息的流程
 */
class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    /**
     * 发送消息
     */
    suspend operator fun invoke(content: String, sender: String): Message {
        return repository.sendMessage(content, sender)
    }
}

/**
 * 获取消息用例
 */
class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    /**
     * 获取所有消息
     */
    operator fun invoke(): Flow<List<Message>> {
        return repository.getAllMessages()
    }

    fun getChatRooms(): Flow<List<ChatRoomSummary>> {
        return repository.getChatRooms()
    }

    suspend fun createChatRoom(peerIp: String): String {
        return repository.createChatRoom(peerIp)
    }

    fun setActiveChatRoom(roomId: String) {
        repository.setActiveChatRoom(roomId)
    }
}

/**
 * 连接服务器用例
 */
class ConnectToServerUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    /**
     * 连接到指定服务器
     */
    operator fun invoke(serverIp: String) {
        repository.connectToServer(serverIp)
    }
}

/**
 * 启动服务器用例
 */
class StartServerUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    /**
     * 启动Socket服务器
     */
    operator fun invoke() {
        repository.startServer()
    }
}

/**
 * 断开连接用例
 */
class DisconnectUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    /**
     * 断开连接
     */
    operator fun invoke() {
        repository.disconnect()
    }
}

/**
 * 获取连接状态用例
 */
class GetConnectionStateUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    /**
     * 获取当前连接状态
     */
    operator fun invoke(): Flow<com.aiim.android.domain.model.ConnectionState> {
        return repository.getConnectionState()
    }
}