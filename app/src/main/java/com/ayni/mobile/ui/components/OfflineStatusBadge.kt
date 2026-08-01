package com.ayni.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ayni.mobile.R
import com.ayni.mobile.ui.theme.OnSurfaceMuted
import com.ayni.mobile.ui.theme.Spacing
import com.ayni.mobile.ui.theme.SurfaceRaised

enum class AiStatus { WARMING_UP, READY, MODEL_MISSING }

/**
 * Comunica que no hay red (a propósito, no es un error) y el estado del modelo IA.
 * Copy con dirección, no disculpa (§6.5): nunca "Error 0x02".
 */
@Composable
fun OfflineStatusBadge(
    aiStatus: AiStatus,
    modifier: Modifier = Modifier
) {
    val text = when (aiStatus) {
        AiStatus.WARMING_UP -> stringResource(R.string.ai_status_warming_up)
        AiStatus.READY -> stringResource(R.string.ai_status_ready)
        AiStatus.MODEL_MISSING -> stringResource(R.string.ai_status_model_missing)
    }
    val icon = when (aiStatus) {
        AiStatus.WARMING_UP -> Icons.Filled.HourglassEmpty
        AiStatus.READY -> Icons.Filled.WifiOff // reutilizado como ícono de "sin red, por diseño"
        AiStatus.MODEL_MISSING -> Icons.Filled.CloudOff
    }

    Surface(
        shape = CircleShape,
        color = SurfaceRaised,
        modifier = modifier.semantics { contentDescription = text }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OnSurfaceMuted,
                modifier = Modifier.padding(end = Spacing.xs)
            )
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
        }
    }
}
