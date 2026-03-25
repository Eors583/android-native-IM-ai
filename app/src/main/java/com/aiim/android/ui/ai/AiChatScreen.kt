package com.aiim.android.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiim.android.domain.ai.OnDeviceModelOption
import com.aiim.chat.R

@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    if (uiState.modelPickerVisible) {
        ModelPickerDialog(
            options = uiState.availableModels,
            selectedId = uiState.pickerSelectedModelId,
            onSelect = viewModel::selectPickerModel,
            onDismiss = viewModel::dismissModelPicker,
            onConfirm = viewModel::confirmModelPickerSelection
        )
    }

    if (uiState.downloadConfirmVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDownloadConfirm,
            title = { Text(text = stringResource(id = R.string.ai_model_download_confirm_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.ai_model_download_confirm_message,
                        uiState.pendingDownloadModelDisplayName
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmModelDownload) {
                    Text(stringResource(id = R.string.ai_model_download_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDownloadConfirm) {
                    Text(stringResource(id = R.string.ai_model_download_confirm_no))
                }
            }
        )
    }

    if (uiState.downloadProgressVisible) {
        val progress = uiState.downloadProgress
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = stringResource(id = R.string.ai_model_downloading_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (progress != null) {
                        Text(
                            text = stringResource(
                                id = R.string.ai_model_downloading_model_format,
                                progress.modelDisplayName
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(
                                id = R.string.ai_model_downloading_file_format,
                                progress.fileLabel,
                                progress.stepLabel
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { progress.overallProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = progress.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::cancelModelDownload) {
                    Text(stringResource(id = R.string.ai_model_download_cancel))
                }
            }
        )
    }

    if (uiState.downloadError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDownloadError,
            title = { Text(text = stringResource(id = R.string.ai_download_error_title)) },
            text = { Text(text = uiState.downloadError ?: "") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDownloadError) {
                    Text(stringResource(id = R.string.ai_model_download_error_ok))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.ai_chat_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(id = R.string.ai_chat_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        if (!uiState.isActiveModelReady) {
            Text(
                text = stringResource(id = R.string.ai_model_not_ready_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    id = R.string.ai_model_current_format,
                    uiState.selectedModelDisplayName.ifBlank { uiState.selectedModelId }
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = viewModel::openModelPicker,
                enabled = !uiState.isGenerating && !uiState.downloadProgressVisible
            ) {
                Text(text = stringResource(id = R.string.ai_model_switch))
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                val bg = if (msg.fromUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.fromUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Text(
                        text = msg.content,
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
        if (uiState.metricsText != null) {
            Text(
                text = uiState.metricsText ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.input,
                onValueChange = viewModel::updateInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(id = R.string.ai_chat_input_hint)) },
                enabled = !uiState.downloadProgressVisible
            )
            if (uiState.isGenerating) {
                TextButton(onClick = viewModel::stopGenerating) {
                    Text(text = stringResource(id = R.string.ai_chat_stop))
                }
            }
            Button(
                onClick = viewModel::send,
                enabled = uiState.input.isNotBlank() &&
                    !uiState.isGenerating &&
                    !uiState.downloadProgressVisible &&
                    uiState.isActiveModelReady
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(2.dp)
                    )
                } else {
                    Text(text = stringResource(id = R.string.ai_chat_send))
                }
            }
        }
    }
}

@Composable
private fun ModelPickerDialog(
    options: List<OnDeviceModelOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.ai_model_picker_title)) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = opt.id == selectedId,
                                onClick = { onSelect(opt.id) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = opt.id == selectedId,
                            onClick = null
                        )
                        Text(
                            text = opt.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedId.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.ai_model_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.ai_model_picker_cancel))
            }
        }
    )
}
