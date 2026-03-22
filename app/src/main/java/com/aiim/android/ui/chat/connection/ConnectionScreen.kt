package com.aiim.android.ui.chat.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aiim.android.domain.model.ConnectionState
import com.aiim.android.ui.chat.ChatViewModel
import com.aiim.android.ui.chat.InputState
import com.aiim.android.ui.chat.ChatUiState
import com.aiim.android.ui.chat.components.ConnectionStatusIndicator
import com.aiim.android.ui.chat.components.NetworkConnectionCard
import com.aiim.android.ui.chat.layout.ChatLayoutSpec
import com.aiim.chat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    uiState: ChatUiState,
    inputState: InputState,
    networkTypeLabel: String,
    viewModel: ChatViewModel,
    layoutSpec: ChatLayoutSpec,
) {
    val scroll = rememberScrollState()
    val connected = uiState.connectionState is ConnectionState.Connected
    val canEnterChat = connected && inputState.nickname.isNotBlank()
    val scheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(16.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outline.copy(alpha = 0.45f),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.primary.copy(alpha = 0.06f),
                        scheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .then(
                    layoutSpec.maxContentWidth?.let { maxW ->
                        Modifier.widthIn(max = maxW)
                    } ?: Modifier,
                )
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(
                    horizontal = layoutSpec.screenPaddingHorizontal,
                    vertical = layoutSpec.screenPaddingVertical,
                ),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = scheme.primaryContainer.copy(alpha = 0.45f),
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = scheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Outlined.Wifi,
                                contentDescription = stringResource(R.string.cd_wifi_icon),
                                tint = scheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.chat_hero_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = scheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.chat_hero_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ConnectionStatusIndicator(uiState.connectionState)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.my_username_label),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = inputState.nickname,
                onValueChange = viewModel::updateNicknameInput,
                label = { Text(stringResource(R.string.nickname_field_label)) },
                placeholder = { Text(stringResource(R.string.nickname_field_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.None,
                ),
            )

            Spacer(modifier = Modifier.height(20.dp))

            NetworkConnectionCard(
                serverIp = inputState.serverIp,
                localHostIp = inputState.localHostIp,
                networkTypeLabel = networkTypeLabel,
                connectionState = uiState.connectionState,
                onServerIpChanged = viewModel::updateServerIpInput,
                onConnect = viewModel::connectToServer,
                onStartServer = viewModel::startServer,
                onDisconnect = viewModel::disconnect,
                onRefreshLocalIp = viewModel::refreshLocalIp,
                modifier = Modifier.fillMaxWidth(),
                fieldShape = fieldShape,
                fieldColors = fieldColors,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::openChatRoom,
                enabled = canEnterChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp,
                    disabledElevation = 0.dp,
                ),
            ) {
                Text(stringResource(R.string.enter_chat_room), style = MaterialTheme.typography.titleMedium)
            }

            if (connected && inputState.nickname.isBlank()) {
                Text(
                    text = stringResource(R.string.need_username_before_chat),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
