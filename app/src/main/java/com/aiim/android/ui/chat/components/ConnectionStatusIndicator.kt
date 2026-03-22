package com.aiim.android.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiim.android.domain.model.ConnectionState
import com.aiim.chat.R

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (dot, bg, labelRes) = when (connectionState) {
        is ConnectionState.Connected -> Triple(
            Color(0xFF10B981),
            scheme.primaryContainer.copy(alpha = 0.55f),
            R.string.status_connected,
        )
        is ConnectionState.Connecting -> Triple(
            Color(0xFFF59E0B),
            scheme.secondaryContainer.copy(alpha = 0.7f),
            R.string.status_connecting,
        )
        is ConnectionState.Failed -> Triple(
            scheme.error,
            scheme.errorContainer.copy(alpha = 0.35f),
            R.string.status_failed,
        )
        else -> Triple(
            scheme.outline,
            scheme.surfaceVariant.copy(alpha = 0.6f),
            R.string.status_disconnected,
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
            )
        }
    }
}
