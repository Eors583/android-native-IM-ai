package com.aiim.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aiim.android.data.local.entity.ChatRoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(room: ChatRoomEntity)

    @Update
    suspend fun updateChatRoom(room: ChatRoomEntity)

    @Query("SELECT * FROM chat_rooms ORDER BY updated_at DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

    @Query("SELECT * FROM chat_rooms WHERE id = :roomId LIMIT 1")
    suspend fun getChatRoomById(roomId: String): ChatRoomEntity?

    @Query("DELETE FROM chat_rooms WHERE id = :roomId")
    suspend fun deleteChatRoomById(roomId: String)
}
