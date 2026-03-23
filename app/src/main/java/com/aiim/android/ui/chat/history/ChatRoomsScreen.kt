package com.aiim.android.ui.chat.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiim.android.domain.model.ChatRoomSummary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatRoomsScreen(
    rooms: List<ChatRoomSummary>,
    onOpenRoom: (String) -> Unit
) {
    val format = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("聊天室", style = MaterialTheme.typography.headlineSmall)
        }
        if (rooms.isEmpty()) {
            item {
                Text(
                    text = "暂无历史聊天室",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(rooms) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRoom(room.id) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(room.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                format.format(room.updatedAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "对端: ${room.peerIp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (room.lastMessagePreview.isBlank()) "暂无消息" else room.lastMessagePreview,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
