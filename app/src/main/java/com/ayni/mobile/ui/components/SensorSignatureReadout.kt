package com.ayni.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayni.mobile.R
import com.ayni.mobile.ui.theme.OnSurfaceMuted
import com.ayni.mobile.ui.theme.Outline
import com.ayni.mobile.ui.theme.Signal
import com.ayni.mobile.ui.theme.Spacing

/**
 * Elemento signature del spec (§6.3): micro-visualización tipo sismógrafo de la
 * aceleración en vivo. "Comunica que el edificio está siendo vigilado ahora mismo" y
 * hace visible la fusión sensor+IA. Si reduced motion está activo, se reemplaza por un
 * readout numérico estático (§6.4) en vez de la onda animada.
 *
 * @param magnitudes buffer de las últimas lecturas (más reciente al final), ya recortado
 * por el caller (ring buffer en el ViewModel, ver StructuralViewModel).
 */
@Composable
fun SensorSignatureReadout(
    magnitudes: List<Float>,
    connected: Boolean,
    isSimulated: Boolean,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val statusText = when {
        !connected -> stringResource(R.string.sensor_readout_disconnected)
        isSimulated -> stringResource(R.string.sensor_readout_simulated)
        else -> stringResource(R.string.sensor_readout_live)
    }
    val lastMagnitud = magnitudes.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (lastMagnitud != null) {
                    "$statusText. Magnitud actual ${"%.2f".format(lastMagnitud)}"
                } else {
                    statusText
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = statusText, style = MaterialTheme.typography.labelMedium, color = OnSurfaceMuted)
            if (lastMagnitud != null) {
                Text(
                    text = "%.2f m/s²".format(lastMagnitud),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (connected) Signal else OnSurfaceMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Reduced motion (§6.4): sin onda animada, solo el número de arriba.
        val showWave = !reducedMotion && magnitudes.size >= 2
        if (showWave) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val color = if (connected) Signal else Outline
                val step = size.width / (magnitudes.size - 1).coerceAtLeast(1)
                val maxMag = (magnitudes.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val midY = size.height / 2f

                var previous: Offset? = null
                magnitudes.forEachIndexed { index, magnitud ->
                    val normalized = ((magnitud - 9.8f) / maxMag).coerceIn(-1f, 1f)
                    val point = Offset(
                        x = index * step,
                        y = midY - (normalized * midY)
                    )
                    previous?.let { start ->
                        drawLine(
                            color = color,
                            start = start,
                            end = point,
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                    previous = point
                }
            }
        }
    }
}
