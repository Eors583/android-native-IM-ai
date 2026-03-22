package com.aiim.android.data.repository

import com.aiim.android.core.im.SocketManager
import com.aiim.android.data.local.dao.MessageDao
import com.aiim.android.data.local.entity.MessageEntity
import com.aiim.android.data.mapper.toDomain
import com.aiim.android.data.mapper.toEntity
import com.aiim.android.data.mapper.toSocketMessage
import com.aiim.android.data.remote.model.SocketMessage
import com.aiim.android.domain.model.ConnectionState
import com.aiim.android.domain.model.Message
import com.aiim.android.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 聊天仓库实现
 * 协调本地数据源（Room）和远程数据源（Socket）
 */
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val socketManager: SocketManager
) : ChatRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        // 监听接收到的Socket消息
        CoroutineScope(Dispatchers.IO).launch {
            socketManager.receivedMessages.collect { socketMessage ->
                socketMessage?.let { onSocketMessageReceived(it) }
            }
        }
    }

    override fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(content: String, sender: String): Message {
        // 创建领域模型
        val message = Message(
            content = content,
            sender = sender,
            isSentByMe = true,
            status = com.aiim.android.domain.model.MessageStatus.SENDING
        )

        // 保存到本地数据库
        messageDao.insertMessage(message.toEntity())

        // 发送到Socket
        val socketMessage = message.toSocketMessage()
        socketManager.sendMessage(socketMessage)

        return message
    }

    override suspend fun updateMessageStatus(messageId: String, status: String) {
        val entity = messageDao.getMessageById(messageId)
        entity?.let {
            val updatedEntity = it.copy(status = status)
            messageDao.updateMessage(updatedEntity)
        }
    }

    override fun startServer() {
        socketManager.startServer()
    }

    override fun connectToServer(serverIp: String) {
        socketManager.connectToServer(serverIp)
    }

    override fun disconnect() {
        socketManager.disconnect()
    }

    override fun getConnectionState(): Flow<ConnectionState> {
        return socketManager.connectionState
    }

    override fun getReceivedMessages(): Flow<SocketMessage?> {
        return socketManager.receivedMessages
    }

    override fun setNickname(nickname: String) {
        socketManager.setNickname(nickname)
    }

    override suspend fun clearAllMessages() {
        messageDao.deleteAllMessages()
    }

    override suspend fun resendFailedMessages() {
        val failedMessages = messageDao.getMessagesByStatus("failed")
        failedMessages.forEach { entity ->
            val socketMessage = entity.toDomain().toSocketMessage()
            socketManager.sendMessage(socketMessage)
        }
    }

    // 私有方法

    private suspend fun onSocketMessageReceived(socketMessage: SocketMessage) {
        // 对端发来的消息在本机一律视为「对方消息」（发送方已在本地插入过 isSentByMe=true）
        val message = socketMessage.toDomain().copy(isSentByMe = false)
        val entity = message.toEntity()
        messageDao.insertMessage(entity)
    }

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }
}