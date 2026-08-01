package com.ayni.mobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ayni.mobile.R
import com.ayni.mobile.ui.theme.AyniOnPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.OnSurfaceMuted
import com.ayni.mobile.ui.theme.Spacing
import com.ayni.mobile.ui.theme.SurfaceRaised

enum class AiStatus { WARMING_UP, READY, MODEL_MISSING }

/**
 * Comunica que no hay red (a propósito, no es un error) y el estado del modelo IA.
 * Copy con dirección, no disculpa (§6.5): nunca "Error 0x02".
 *
 * Cuando `aiStatus == MODEL_MISSING` y se pasa `onPickModel`, el tag se vuelve
 * **accionable**: cambia a ámbar, late suavemente (heartbeat) para llamar la atención, y
 * tocarlo abre el selector de archivos directo — no hace falta bajar a leer el banner de
 * detalle para encontrar el botón.
 */
@Composable
fun OfflineStatusBadge(
    aiStatus: AiStatus,
    modifier: Modifier = Modifier,
    onPickModel: (() -> Unit)? = null
) {
    val actionable = aiStatus == AiStatus.MODEL_MISSING && onPickModel != null

    val text = when (aiStatus) {
        AiStatus.WARMING_UP -> stringResource(R.string.ai_status_warming_up)
        AiStatus.READY -> stringResource(R.string.ai_status_ready)
        AiStatus.MODEL_MISSING -> if (actionable) {
            stringResource(R.string.ai_status_model_missing_actionable)
        } else {
            stringResource(R.string.ai_status_model_missing)
        }
    }
    val icon = when (aiStatus) {
        AiStatus.WARMING_UP -> Icons.Filled.HourglassEmpty
        AiStatus.READY -> Icons.Filled.WifiOff // reutilizado como ícono de "sin red, por diseño"
        AiStatus.MODEL_MISSING -> if (actionable) Icons.Filled.FolderOpen else Icons.Filled.CloudOff
    }

    val heartbeat = rememberInfiniteTransition(label = "ai-tag-heartbeat")
    val scale by heartbeat.animateFloat(
        initialValue = 1f,
        targetValue = if (actionable) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heartbeat-scale"
    )

    val containerColor = if (actionable) AyniPrimaryFixed else SurfaceRaised
    val contentColor = if (actionable) AyniOnPrimaryContainer else OnSurfaceMuted

    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (actionable) {
                    Modifier.clickable(onClick = onPickModel)
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = text }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.padding(end = Spacing.xs)
            )
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}
