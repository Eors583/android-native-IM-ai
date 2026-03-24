package com.aiim.android.ui.chat.chatroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiim.android.domain.model.ConnectionState
import com.aiim.android.ui.chat.ChatUiState
import com.aiim.android.ui.chat.ChatViewModel
import com.aiim.android.ui.chat.InputState
import com.aiim.android.ui.chat.components.ChatRoomMessageInputBar
import com.aiim.android.ui.chat.components.ConnectionStatusIndicator
import com.aiim.android.ui.chat.components.MessageListSection
import com.aiim.android.ui.chat.layout.ChatLayoutSpec
import com.aiim.chat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    uiState: ChatUiState,
    inputState: InputState,
    viewModel: ChatViewModel,
    layoutSpec: ChatLayoutSpec,
) {
    LaunchedEffect(uiState.messages) {
        viewModel.acknowledgePeerMessagesReadIfNeeded(uiState.messages)
    }
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .then(
                    layoutSpec.maxContentWidth?.let { maxW ->
                        Modifier.widthIn(max = maxW)
                    } ?: Modifier,
                )
                .fillMaxWidth(),
        ) {
            Surface(
                tonalElevation = 2.dp,
                color = scheme.surfaceContainerLow,
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.chat_room_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = scheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.sending_as_format, inputState.nickname),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::backToConnection) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = scheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        ConnectionStatusIndicator(
                            uiState.connectionState,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = scheme.onSurface,
                        navigationIconContentColor = scheme.onSurface,
                        actionIconContentColor = scheme.onSurface,
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerLow.copy(alpha = 0.5f)),
            ) {
                MessageListSection(
                    messages = uiState.messages,
                    bubbleMaxFraction = layoutSpec.messageBubbleMaxFraction,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Surface(
                color = scheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                ChatRoomMessageInputBar(
                    viewModel = viewModel,
                    nickname = inputState.nickname,
                    isConnected = uiState.connectionState is ConnectionState.Connected,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}
