package com.aiim.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aiim.android.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 消息数据访问对象（DAO）
 * 提供对messages表的CRUD操作
 */
@Dao
interface MessageDao {

    /**
     * 插入新消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * 插入多条消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * 更新消息状态
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)

    /**
     * 获取所有消息（按时间倒序）
     */
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE room_id = :roomId ORDER BY timestamp DESC")
    fun getMessagesByRoom(roomId: String): Flow<List<MessageEntity>>

    /**
     * 获取指定时间之后的消息
     */
    @Query("SELECT * FROM messages WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getMessagesSince(since: Date): Flow<List<MessageEntity>>

    /**
     * 获取未发送成功的消息
     */
    @Query("SELECT * FROM messages WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getMessagesByStatus(status: String): List<MessageEntity>

    /**
     * 根据ID获取消息
     */
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    /**
     * 删除所有消息
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    /**
     * 删除指定时间之前的消息
     */
    @Query("DELETE FROM messages WHERE timestamp < :before")
    suspend fun deleteMessagesBefore(before: Date)

    @Query("DELETE FROM messages WHERE room_id = :roomId")
    suspend fun deleteMessagesByRoomId(roomId: String)

    /**
     * 将会话内我方发送的、且不晚于锚点时间的消息标记为已读（对端 read_ack）
     */
    @Query(
        """
        UPDATE messages SET status = :readStatus
        WHERE room_id = :roomId
        AND is_sent_by_me = 1
        AND timestamp <= :beforeInclusive
        AND status != 'FAILED'
        """
    )
    suspend fun markMyMessagesReadUpTo(
        roomId: String,
        beforeInclusive: Date,
        readStatus: String
    )

    /**
     * 获取消息数量
     */
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int
}