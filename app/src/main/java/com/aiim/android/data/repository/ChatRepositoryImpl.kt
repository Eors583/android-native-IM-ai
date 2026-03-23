package com.aiim.android.data.repository

import com.aiim.android.core.im.SocketManager
import com.aiim.android.data.local.dao.ChatRoomDao
import com.aiim.android.data.local.dao.MessageDao
import com.aiim.android.data.local.entity.ChatRoomEntity
import com.aiim.android.data.mapper.toDomain
import com.aiim.android.data.mapper.toEntity
import com.aiim.android.data.mapper.toSocketMessage
import com.aiim.android.data.remote.model.SocketMessage
import com.aiim.android.domain.model.ChatRoomSummary
import com.aiim.android.domain.model.ConnectionState
import com.aiim.android.domain.model.Message
import com.aiim.android.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * 聊天仓库实现
 * 协调本地数据源（Room）和远程数据源（Socket）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val chatRoomDao: ChatRoomDao,
    private val socketManager: SocketManager
) : ChatRepository {

    private val activeRoomId = MutableStateFlow("")

    init {
        // 监听接收到的Socket消息
        CoroutineScope(Dispatchers.IO).launch {
            socketManager.receivedMessages.collect { socketMessage ->
                socketMessage?.let { onSocketMessageReceived(it) }
            }
        }
    }

    override fun getAllMessages(): Flow<List<Message>> {
        return activeRoomId.flatMapLatest { roomId ->
            if (roomId.isBlank()) {
                flowOf(emptyList())
            } else {
                messageDao.getMessagesByRoom(roomId)
            }
        }.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getChatRooms(): Flow<List<ChatRoomSummary>> {
        return chatRoomDao.getAllChatRooms().map { rooms -> rooms.map { it.toDomain() } }
    }

    override suspend fun createChatRoom(peerIp: String): String {
        val now = Date()
        val roomId = UUID.randomUUID().toString()
        val title = "聊天室 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(now)}"
        chatRoomDao.insertChatRoom(
            ChatRoomEntity(
                id = roomId,
                title = title,
                peerIp = peerIp,
                lastMessagePreview = "",
                createdAt = now,
                updatedAt = now
            )
        )
        activeRoomId.value = roomId
        return roomId
    }

    override fun setActiveChatRoom(roomId: String) {
        activeRoomId.value = roomId
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
        val roomId = activeRoomId.value
        if (roomId.isBlank()) {
            throw IllegalStateException("当前未选择聊天室")
        }
        messageDao.insertMessage(message.toEntity(roomId))
        touchRoomByMessage(roomId, content)

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
        val roomId = activeRoomId.value
        if (roomId.isBlank()) return
        val message = socketMessage.toDomain().copy(isSentByMe = false)
        val entity = message.toEntity(roomId)
        messageDao.insertMessage(entity)
        touchRoomByMessage(roomId, message.content)
    }

    private suspend fun touchRoomByMessage(roomId: String, message: String) {
        val room = chatRoomDao.getChatRoomById(roomId) ?: return
        val preview = message.take(60)
        chatRoomDao.updateChatRoom(
            room.copy(
                lastMessagePreview = preview,
                updatedAt = Date()
            )
        )
    }
}