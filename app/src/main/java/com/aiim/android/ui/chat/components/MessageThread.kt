package com.aiim.android.ui.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aiim.android.domain.model.Message
import com.aiim.android.domain.model.MessageStatus
import com.aiim.chat.R
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessageListSection(
    messages: List<Message>,
    bubbleMaxFraction: Float,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        reverseLayout = false,
    ) {
        if (messages.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(88.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = stringResource(R.string.cd_empty_chat),
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.empty_messages_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.empty_messages_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(messages) { message ->
                MessageItem(
                    message = message,
                    bubbleMaxFraction = bubbleMaxFraction,
                    timeFormat = timeFormat,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    bubbleMaxFraction: Float,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isSentByMe = message.isSentByMe
    val bubbleShape = RoundedCornerShape(
        topStart = if (isSentByMe) 18.dp else 6.dp,
        topEnd = if (isSentByMe) 6.dp else 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 18.dp,
    )
    val alignment = if (isSentByMe) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(bubbleMaxFraction)
                .padding(vertical = 2.dp),
            shape = bubbleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSentByMe) {
                    scheme.primaryContainer
                } else {
                    scheme.surfaceContainerHighest
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (!isSentByMe) {
                BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f))
            } else {
                null
            },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (!isSentByMe && message.messageType != "system") {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSentByMe) {
                        val statusColor = when (message.status) {
                            MessageStatus.SENDING -> scheme.onSurfaceVariant
                            MessageStatus.SENT -> scheme.primary
                            MessageStatus.DELIVERED -> Color(0xFF059669)
                            MessageStatus.READ -> Color(0xFF0369A1)
                            MessageStatus.FAILED -> scheme.error
                        }
                        Text(
                            text = when (message.status) {
                                MessageStatus.SENDING -> stringResource(R.string.msg_status_sending)
                                MessageStatus.SENT -> stringResource(R.string.msg_status_sent)
                                MessageStatus.DELIVERED -> stringResource(R.string.msg_status_delivered)
                                MessageStatus.READ -> stringResource(R.string.msg_status_read)
                                MessageStatus.FAILED -> stringResource(R.string.msg_status_failed)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = timeFormat.format(message.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (message.messageType == "system") {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                textAlign = if (isSentByMe) TextAlign.End else TextAlign.Center,
            )
        }
    }
}
