package com.aiim.android.data.mapper

import com.aiim.android.core.utils.Constants
import com.aiim.android.data.local.entity.MessageEntity
import com.aiim.android.data.remote.model.SocketMessage
import com.aiim.android.domain.model.Message
import com.aiim.android.domain.model.MessageStatus

/**
 * 数据映射器：在不同层之间转换数据模型（文件级扩展，便于按成员导入）。
 */
fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        content = content,
        sender = sender,
        timestamp = timestamp,
        status = status.name,
        isSentByMe = isSentByMe,
        messageType = messageType
    )
}

fun MessageEntity.toDomain(): Message {
    return Message(
        id = id,
        content = content,
        sender = sender,
        timestamp = timestamp,
        status = MessageStatus.valueOf(status),
        isSentByMe = isSentByMe,
        messageType = messageType
    )
}

fun Message.toSocketMessage(): SocketMessage {
    return SocketMessage(
        id = id,
        content = content,
        sender = sender,
        timestamp = timestamp.time,
        status = status.name.lowercase(),
        isSentByMe = isSentByMe,
        messageType = messageType
    )
}

fun SocketMessage.toDomain(): Message {
    return Message(
        id = id,
        content = content,
        sender = sender,
        timestamp = java.util.Date(timestamp),
        status = when (status.lowercase()) {
            "sending" -> MessageStatus.SENDING
            "sent" -> MessageStatus.SENT
            "delivered" -> MessageStatus.DELIVERED
            "failed" -> MessageStatus.FAILED
            else -> MessageStatus.SENT
        },
        isSentByMe = isSentByMe,
        messageType = messageType
    )
}

fun MessageEntity.toSocketMessage(): SocketMessage {
    return SocketMessage(
        id = id,
        content = content,
        sender = sender,
        timestamp = timestamp.time,
        status = status,
        isSentByMe = isSentByMe,
        messageType = messageType
    )
}

fun SocketMessage.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        content = content,
        sender = sender,
        timestamp = java.util.Date(timestamp),
        status = status,
        isSentByMe = isSentByMe,
        messageType = messageType
    )
}
