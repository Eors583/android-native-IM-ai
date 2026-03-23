package com.aiim.android.domain.model

import java.util.Date

data class ChatRoomSummary(
    val id: String,
    val title: String,
    val peerIp: String,
    val lastMessagePreview: String,
    val createdAt: Date,
    val updatedAt: Date
)
