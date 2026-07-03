package com.voxly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voxly.app.R
import com.voxly.app.data.model.SpeakerAvailabilityUi
import com.voxly.app.data.model.SpeakerStatus

fun SpeakerStatus.displayTextRes(): Pair<Int, Int> {
    return when (this) {
        SpeakerStatus.OFFLINE -> Pair(R.string.status_offline, 0)
        SpeakerStatus.AVAILABLE -> Pair(R.string.status_online, R.string.status_available)
        SpeakerStatus.UNAVAILABLE -> Pair(R.string.status_online, R.string.status_not_accepting)
        SpeakerStatus.RINGING -> Pair(R.string.status_receiving_call, 0)
        SpeakerStatus.CONNECTING -> Pair(R.string.status_connecting, 0)
        SpeakerStatus.IN_CALL -> Pair(R.string.status_in_call, 0)
    }
}

fun SpeakerStatus.getStatusEmoji(): String {
    return when (this) {
        SpeakerStatus.OFFLINE -> "⚫"
        SpeakerStatus.AVAILABLE -> "🟢"
        SpeakerStatus.UNAVAILABLE -> "🟢"
        SpeakerStatus.RINGING -> "🟡"
        SpeakerStatus.CONNECTING -> "🔵"
        SpeakerStatus.IN_CALL -> "🔴"
    }
}

fun SpeakerStatus.getStatusColor(): Color {
    return when (this) {
        SpeakerStatus.OFFLINE -> Color.Gray
        SpeakerStatus.AVAILABLE -> Color(0xFF4CAF50)
        SpeakerStatus.UNAVAILABLE -> Color(0xFFFF9800) // Orange status indicator
        SpeakerStatus.RINGING -> Color(0xFFFFEB3B)
        SpeakerStatus.CONNECTING -> Color(0xFF2196F3)
        SpeakerStatus.IN_CALL -> Color(0xFFE53935)
    }
}

@Composable
fun SpeakerCallActions(
    availability: SpeakerAvailabilityUi,
    audioRate: Int,
    videoRate: Int,
    showStatusText: Boolean = true,
    showPricing: Boolean = true,
    compactLayout: Boolean = false,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    if (compactLayout) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Audio Button
            IconButton(
                onClick = onCallClick,
                enabled = availability.audioEnabled,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF1E1D26), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .graphicsLayer(alpha = if (availability.audioEnabled) 1f else 0.38f)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Video Button
            IconButton(
                onClick = onVideoCallClick,
                enabled = availability.videoEnabled,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF1E1D26), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .graphicsLayer(alpha = if (availability.videoEnabled) 1f else 0.38f)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Video Call",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        val (primaryRes, secondaryRes) = availability.status.displayTextRes()
        val emoji = availability.status.getStatusEmoji()
        val primaryText = "$emoji " + stringResource(id = primaryRes)
        val secondaryText = if (secondaryRes != 0) stringResource(id = secondaryRes) else ""

        Column {
            if (showStatusText) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = primaryText, style = MaterialTheme.typography.bodyMedium)
                    if (secondaryText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($secondaryText)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCallClick,
                    enabled = availability.audioEnabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).graphicsLayer(alpha = if (availability.audioEnabled) 1f else 0.38f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call")
                    Spacer(modifier = Modifier.width(4.dp))
                    if (showPricing) {
                        Text(
                            text = "$audioRate 🪙/min",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Text(text = "Call", maxLines = 1)
                    }
                }

                Button(
                    onClick = onVideoCallClick,
                    enabled = availability.videoEnabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).graphicsLayer(alpha = if (availability.videoEnabled) 1f else 0.38f)
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = "Video Call")
                    Spacer(modifier = Modifier.width(4.dp))
                    if (showPricing) {
                        Text(
                            text = "$videoRate 🪙/min",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Text(text = "Video", maxLines = 1)
                    }
                }
            }
        }
    }
}
