package com.olorin.claudette.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olorin.claudette.models.NetworkStatus
import com.olorin.claudette.ui.theme.ClaudetteSurfaceVariant

@Composable
fun ConnectionStatusHud(
    networkStatus: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (networkStatus) {
        is NetworkStatus.Reachable -> Color(0xFF22C55E) to "${networkStatus.latencyMs.toInt()}ms"
        is NetworkStatus.Degraded -> Color(0xFFF59E0B) to "${networkStatus.latencyMs.toInt()}ms"
        is NetworkStatus.Unreachable -> Color(0xFFEF4444) to "Offline"
        is NetworkStatus.Unknown -> Color(0xFF6B7280) to "..."
    }

    val icon = when (networkStatus) {
        is NetworkStatus.Unreachable -> Icons.Default.SignalCellularConnectedNoInternet0Bar
        else -> Icons.Default.SignalCellularAlt
    }

    Row(
        modifier = modifier
            .background(ClaudetteSurfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(8.dp)
        )
        Icon(
            imageVector = icon,
            contentDescription = "Network status",
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}
