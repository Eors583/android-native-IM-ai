package com.aiim.android.domain.repository

import com.aiim.android.data.remote.model.SocketMessage
import com.aiim.android.domain.model.ConnectionState
import com.aiim.android.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库接口（领域层）
 * 定义领域层需要的操作，不依赖具体实现
 */
interface ChatRepository {

    /**
     * 获取所有消息
     */
    fun getAllMessages(): Flow<List<Message>>

    /**
     * 发送消息
     */
    suspend fun sendMessage(content: String, sender: String): Message

    /**
     * 更新消息状态
     */
    suspend fun updateMessageStatus(messageId: String, status: String)

    /**
     * 启动服务器
     */
    fun startServer()

    /**
     * 连接到服务器
     */
    fun connectToServer(serverIp: String)

    /**
     * 断开连接
     */
    fun disconnect()

    /**
     * 获取连接状态
     */
    fun getConnectionState(): Flow<ConnectionState>

    /**
     * 获取接收到的 Socket 消息流（数据层推送）
     */
    fun getReceivedMessages(): Flow<SocketMessage?>

    /**
     * 设置用户昵称
     */
    fun setNickname(nickname: String)

    /**
     * 清除所有消息
     */
    suspend fun clearAllMessages()

    /**
     * 重新发送失败的消息
     */
    suspend fun resendFailedMessages()
}