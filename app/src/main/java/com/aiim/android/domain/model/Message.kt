package com.aiim.android.domain.model

import com.aiim.android.core.utils.Constants
import java.util.Date
import java.util.UUID

/**
 * 消息数据模型
 * 用于表示聊天消息的领域模型
 *
 * @property id 消息唯一标识
 * @property content 消息内容
 * @property sender 发送者昵称
 * @property timestamp 发送时间戳
 * @property status 消息状态（发送中/已发送/已送达/发送失败）
 * @property isSentByMe 是否由当前用户发送
 * @property messageType 消息类型（文本/心跳/系统）
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val sender: String,
    val timestamp: Date = Date(),
    val status: MessageStatus = MessageStatus.SENDING,
    val isSentByMe: Boolean = false,
    val messageType: String = Constants.MESSAGE_TYPE_TEXT
) {
    companion object {
        /**
         * 创建系统消息
         */
        fun createSystemMessage(content: String): Message {
            return Message(
                content = content,
                sender = "系统",
                messageType = Constants.MESSAGE_TYPE_SYSTEM,
                isSentByMe = false
            )
        }

        /**
         * 创建心跳消息
         */
        fun createHeartbeatMessage(): Message {
            return Message(
                content = "heartbeat",
                sender = "系统",
                messageType = Constants.MESSAGE_TYPE_HEARTBEAT,
                isSentByMe = false
            )
        }
    }
}

/**
 * 消息状态枚举
 */
enum class MessageStatus {
    SENDING,    // 发送中
    SENT,       // 已发送（已从本机写入 socket）
    DELIVERED,  // 已送达（对端已收到）
    READ,       // 已读（对端已查看会话）
    FAILED      // 发送失败
}

/**
 * 连接状态密封类
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Failed(val error: String) : ConnectionState()

    override fun toString(): String {
        return when (this) {
            is Disconnected -> Constants.CONNECTION_STATE_DISCONNECTED
            is Connecting -> Constants.CONNECTION_STATE_CONNECTING
            is Connected -> Constants.CONNECTION_STATE_CONNECTED
            is Failed -> Constants.CONNECTION_STATE_FAILED
        }
    }
}