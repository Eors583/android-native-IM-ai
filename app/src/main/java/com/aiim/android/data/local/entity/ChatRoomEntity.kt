package com.aiim.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "peer_ip")
    val peerIp: String,
    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date
)
