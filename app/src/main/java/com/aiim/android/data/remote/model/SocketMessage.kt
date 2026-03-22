package com.aiim.android.data.remote.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Socket通信消息模型
 * 用于JSON序列化和反序列化
 */
data class SocketMessage(
    @SerializedName("id")
    val id: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("sender")
    val sender: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("status")
    val status: String,

    @SerializedName("is_sent_by_me")
    val isSentByMe: Boolean,

    @SerializedName("message_type")
    val messageType: String
) {
    companion object {
        /**
         * 创建心跳消息
         */
        fun createHeartbeat(sender: String): SocketMessage {
            return SocketMessage(
                id = "heartbeat_${System.currentTimeMillis()}",
                content = "heartbeat",
                sender = sender,
                timestamp = System.currentTimeMillis(),
                status = "sent",
                isSentByMe = false,
                messageType = "heartbeat"
            )
        }

        /**
         * 创建系统消息
         */
        fun createSystemMessage(content: String): SocketMessage {
            return SocketMessage(
                id = "system_${System.currentTimeMillis()}",
                content = content,
                sender = "系统",
                timestamp = System.currentTimeMillis(),
                status = "sent",
                isSentByMe = false,
                messageType = "system"
            )
        }
    }
}