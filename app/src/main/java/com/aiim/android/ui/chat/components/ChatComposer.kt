package com.aiim.android.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aiim.android.ui.chat.ChatViewModel
import com.aiim.chat.R

/**
 * 聊天室底部输入：本地 [TextFieldValue] 保留 IME composition（中文拼音组字）。
 */
@Composable
fun ChatRoomMessageInputBar(
    viewModel: ChatViewModel,
    nickname: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    val draft = textFieldValue.text
    val canSend = draft.isNotBlank() && isConnected && nickname.isNotBlank()
    val scheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(22.dp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    placeholder = { Text(stringResource(R.string.message_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    minLines = 1,
                    maxLines = 5,
                    shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline.copy(alpha = 0.35f),
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default,
                        capitalization = KeyboardCapitalization.None,
                    ),
                )

                FilledIconButton(
                    onClick = {
                        viewModel.sendMessageWithContent(draft) { err ->
                            if (err == null) {
                                textFieldValue = TextFieldValue()
                            }
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.surfaceVariant,
                        disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (!isConnected) {
                Text(
                    text = stringResource(R.string.disconnected_go_back_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                    modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                )
            }
        }
    }
}
