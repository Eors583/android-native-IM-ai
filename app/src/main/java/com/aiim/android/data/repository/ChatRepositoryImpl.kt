package com.aiim.android.data.repository

import com.aiim.android.core.im.SocketManager
import com.aiim.android.data.local.dao.ChatRoomDao
import com.aiim.android.data.local.dao.MessageDao
import com.aiim.android.data.local.entity.ChatRoomEntity
import com.aiim.android.data.mapper.toDomain
import com.aiim.android.data.mapper.toEntity
import com.aiim.android.data.mapper.toSocketMessage
import com.aiim.android.data.remote.model.SocketMessage
import com.aiim.android.core.utils.Constants
import com.aiim.android.domain.model.ChatRoomSummary
import com.aiim.android.domain.model.ConnectionState
import com.aiim.android.domain.model.Message
import com.aiim.android.domain.model.MessageStatus
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

    override suspend fun deleteChatRoom(roomId: String) {
        messageDao.deleteMessagesByRoomId(roomId)
        chatRoomDao.deleteChatRoomById(roomId)
        if (activeRoomId.value == roomId) {
            activeRoomId.value = ""
        }
    }

    override suspend fun sendMessage(content: String, sender: String): Message {
        // 创建领域模型
        val message = Message(
            content = content,
            sender = sender,
            isSentByMe = true,
            status = MessageStatus.SENDING
        )

        // 保存到本地数据库
        val roomId = activeRoomId.value
        if (roomId.isBlank()) {
            throw IllegalStateException("当前未选择聊天室")
        }
        messageDao.insertMessage(message.toEntity(roomId))
        touchRoomByMessage(roomId, content)

        // 发送到 Socket（挂起直到写入完成，并更新送达状态）
        val socketMessage = message.toSocketMessage()
        val ok = socketManager.sendMessage(socketMessage)
        val entity = messageDao.getMessageById(message.id)
        if (entity != null) {
            val newStatus = if (ok) MessageStatus.SENT else MessageStatus.FAILED
            messageDao.updateMessage(entity.copy(status = newStatus.name))
        }

        return message.copy(status = if (ok) MessageStatus.SENT else MessageStatus.FAILED)
    }

    override suspend fun updateMessageStatus(messageId: String, status: String) {
        val entity = messageDao.getMessageById(messageId)
        entity?.let {
            val updatedEntity = it.copy(status = status)
            messageDao.updateMessage(updatedEntity)
        }
    }

    override suspend fun sendReadReceipt(anchorMessageId: String) {
        val trimmed = anchorMessageId.trim()
        if (trimmed.isEmpty()) return
        val roomId = activeRoomId.value
        if (roomId.isBlank()) return
        val anchor = messageDao.getMessageById(trimmed) ?: return
        if (anchor.roomId != roomId) return
        if (anchor.isSentByMe) return
        val ack = SocketMessage(
            id = UUID.randomUUID().toString(),
            content = trimmed,
            sender = socketManager.getNickname(),
            timestamp = System.currentTimeMillis(),
            status = "sent",
            isSentByMe = false,
            messageType = Constants.MESSAGE_TYPE_READ_ACK
        )
        socketManager.sendMessage(ack)
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
        val failedMessages = messageDao.getMessagesByStatus(MessageStatus.FAILED.name)
        for (entity in failedMessages) {
            val socketMessage = entity.toDomain().toSocketMessage()
            val ok = socketManager.sendMessage(socketMessage)
            if (ok) {
                messageDao.updateMessage(entity.copy(status = MessageStatus.SENT.name))
            }
        }
    }

    // 私有方法

    private suspend fun onSocketMessageReceived(socketMessage: SocketMessage) {
        when (socketMessage.messageType) {
            Constants.MESSAGE_TYPE_DELIVERY_ACK -> {
                handleDeliveryAck(socketMessage.content.trim())
                return
            }
            Constants.MESSAGE_TYPE_READ_ACK -> {
                handleReadAck(socketMessage.content.trim())
                return
            }
        }

        val roomId = activeRoomId.value
        if (roomId.isBlank()) return

        // 对端发来的聊天内容在本机一律视为「对方消息」
        val message = socketMessage.toDomain().copy(isSentByMe = false)
        val entity = message.toEntity(roomId)
        messageDao.insertMessage(entity)
        touchRoomByMessage(roomId, message.content)

        if (socketMessage.messageType == Constants.MESSAGE_TYPE_TEXT) {
            sendDeliveryAck(socketMessage.id)
        }
    }

    private suspend fun handleDeliveryAck(messageId: String) {
        if (messageId.isBlank()) return
        val roomId = activeRoomId.value
        if (roomId.isBlank()) return
        val entity = messageDao.getMessageById(messageId) ?: return
        if (entity.roomId != roomId) return
        if (!entity.isSentByMe) return
        val next = when (entity.toDomain().status) {
            MessageStatus.READ -> MessageStatus.READ
            else -> MessageStatus.DELIVERED
        }
        messageDao.updateMessage(entity.copy(status = next.name))
    }

    private suspend fun handleReadAck(anchorMessageId: String) {
        if (anchorMessageId.isBlank()) return
        val roomId = activeRoomId.value
        if (roomId.isBlank()) return
        val anchor = messageDao.getMessageById(anchorMessageId) ?: return
        if (anchor.roomId != roomId) return
        if (anchor.isSentByMe) return
        messageDao.markMyMessagesReadUpTo(
            roomId = roomId,
            beforeInclusive = anchor.timestamp,
            readStatus = MessageStatus.READ.name
        )
    }

    private suspend fun sendDeliveryAck(remoteMessageId: String) {
        if (remoteMessageId.isBlank()) return
        val ack = SocketMessage(
            id = UUID.randomUUID().toString(),
            content = remoteMessageId,
            sender = socketManager.getNickname(),
            timestamp = System.currentTimeMillis(),
            status = "sent",
            isSentByMe = false,
            messageType = Constants.MESSAGE_TYPE_DELIVERY_ACK
        )
        socketManager.sendMessage(ack)
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