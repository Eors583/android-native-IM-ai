package com.aiim.android.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aiim.android.domain.model.ConnectionState
import com.aiim.chat.R

@Composable
fun NetworkConnectionCard(
    serverIp: String,
    localHostIp: String,
    networkTypeLabel: String,
    connectionState: ConnectionState,
    onServerIpChanged: (String) -> Unit,
    onConnect: () -> Unit,
    onStartServer: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshLocalIp: () -> Unit,
    modifier: Modifier = Modifier,
    fieldShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    fieldColors: TextFieldColors? = null,
) {
    val canOperateDisconnect = connectionState is ConnectionState.Connected ||
        connectionState is ConnectionState.Connecting ||
        connectionState is ConnectionState.Failed
    val scheme = MaterialTheme.colorScheme
    val resolvedFieldColors = fieldColors ?: OutlinedTextFieldDefaults.colors(
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outline.copy(alpha = 0.45f),
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.network_connection_title),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.network_connection_help),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.local_ipv4_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = localHostIp,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.primary,
                    )
                }
                TextButton(onClick = onRefreshLocalIp) {
                    Text(stringResource(R.string.refresh))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.current_network_format, networkTypeLabel),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.network_troubleshoot),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = serverIp,
                onValueChange = onServerIpChanged,
                label = { Text(stringResource(R.string.peer_ip_label)) },
                placeholder = { Text(stringResource(R.string.peer_ip_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = fieldShape,
                colors = resolvedFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.None,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onConnect,
                    enabled = serverIp.isNotEmpty() &&
                        connectionState !is ConnectionState.Connected &&
                        connectionState !is ConnectionState.Connecting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.action_connect), style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = onStartServer,
                    enabled = connectionState !is ConnectionState.Connected &&
                        connectionState !is ConnectionState.Connecting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.action_start_server), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDisconnect,
                enabled = canOperateDisconnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.action_disconnect))
            }
        }
    }
}
