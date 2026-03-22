package com.aiim.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room数据库消息实体
 * 对应数据库中的messages表
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Date,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "is_sent_by_me")
    val isSentByMe: Boolean,

    @ColumnInfo(name = "message_type")
    val messageType: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date()
)