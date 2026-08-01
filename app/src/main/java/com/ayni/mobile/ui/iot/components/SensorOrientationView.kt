package com.ayni.mobile.ui.iot.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private data class Point3(
    val x: Float,
    val y: Float,
    val z: Float,
)

private data class ProjectedPoint(
    val position: Offset,
    val depth: Float,
)

private data class Face(
    val vertices: IntArray,
    val color: Color,
)

@Composable
fun SensorOrientationView(
    rollDeg: Double,
    pitchDeg: Double,
    modifier: Modifier = Modifier,
) {
    val animatedRoll by animateFloatAsState(
        targetValue = rollDeg.toFloat(),
        animationSpec = tween(450),
        label = "roll",
    )
    val animatedPitch by animateFloatAsState(
        targetValue = pitchDeg.toFloat(),
        animationSpec = tween(450),
        label = "pitch",
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    val darkPalette = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = 12.dp),
        ) {
            drawSensorBox(
                rollDeg = animatedRoll,
                pitchDeg = animatedPitch,
                primary = primary,
                secondary = secondary,
                outline = outline,
                surface = surface,
                darkPalette = darkPalette,
            )
        }
        Text(
            text = "roll ${formatAngle(rollDeg)}°   ·   pitch ${formatAngle(pitchDeg)}°",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "La vista usa gravedad; yaw no es absoluto.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun DrawScope.drawSensorBox(
    rollDeg: Float,
    pitchDeg: Float,
    primary: Color,
    secondary: Color,
    outline: Color,
    surface: Color,
    darkPalette: Boolean,
) {
    val vertices = listOf(
        Point3(-1.15f, -0.72f, -0.18f),
        Point3(1.15f, -0.72f, -0.18f),
        Point3(1.15f, 0.72f, -0.18f),
        Point3(-1.15f, 0.72f, -0.18f),
        Point3(-1.15f, -0.72f, 0.18f),
        Point3(1.15f, -0.72f, 0.18f),
        Point3(1.15f, 0.72f, 0.18f),
        Point3(-1.15f, 0.72f, 0.18f),
    )
    val scale = size.minDimension * 0.30f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Halo y retícula: aportan profundidad sin fingir datos adicionales.
    drawCircle(
        color = primary.copy(alpha = if (darkPalette) 0.13f else 0.07f),
        radius = size.minDimension * 0.39f,
        center = center,
    )
    drawCircle(
        color = secondary.copy(alpha = if (darkPalette) 0.11f else 0.06f),
        radius = size.minDimension * 0.28f,
        center = center,
    )
    drawCircle(
        color = outline.copy(alpha = if (darkPalette) 0.30f else 0.18f),
        radius = size.minDimension * 0.34f,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
    )
    drawLine(
        color = outline.copy(alpha = if (darkPalette) 0.24f else 0.13f),
        start = Offset(center.x, center.y - size.minDimension * 0.39f),
        end = Offset(center.x, center.y + size.minDimension * 0.39f),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = outline.copy(alpha = if (darkPalette) 0.24f else 0.13f),
        start = Offset(center.x - size.minDimension * 0.45f, center.y),
        end = Offset(center.x + size.minDimension * 0.45f, center.y),
        strokeWidth = 1.dp.toPx(),
    )
    val projected = vertices.map {
        project(
            point = rotate(it, rollDeg, pitchDeg),
            center = center,
            scale = scale,
        )
    }
    val faces = listOf(
        Face(intArrayOf(0, 1, 2, 3), surface.copy(alpha = 0.94f)),
        Face(intArrayOf(4, 5, 6, 7), primary.copy(alpha = 0.88f)),
        Face(intArrayOf(0, 1, 5, 4), secondary.copy(alpha = 0.56f)),
        Face(intArrayOf(1, 2, 6, 5), primary.copy(alpha = 0.66f)),
        Face(intArrayOf(2, 3, 7, 6), secondary.copy(alpha = 0.46f)),
        Face(intArrayOf(3, 0, 4, 7), primary.copy(alpha = 0.60f)),
    ).sortedBy { face ->
        face.vertices.map { projected[it].depth }.average()
    }

    faces.forEach { face ->
        val path = Path()
        face.vertices.forEachIndexed { index, vertexIndex ->
            val point = projected[vertexIndex].position
            if (index == 0) path.moveTo(point.x, point.y)
            else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, face.color)
        drawPath(path, outline, style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f))
    }

    // Punto de referencia del centro del IMU sobre la placa.
    val topCenter = project(
        rotate(Point3(0f, 0f, 0.20f), rollDeg, pitchDeg),
        center,
        scale,
    ).position
    drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        radius = 8.dp.toPx(),
        center = topCenter,
    )
    drawCircle(
        color = primary,
        radius = 4.dp.toPx(),
        center = topCenter,
    )
}

private fun rotate(point: Point3, rollDeg: Float, pitchDeg: Float): Point3 {
    val roll = Math.toRadians(rollDeg.toDouble())
    val pitch = Math.toRadians(pitchDeg.toDouble())

    val afterRoll = Point3(
        x = point.x,
        y = (point.y * cos(roll) - point.z * sin(roll)).toFloat(),
        z = (point.y * sin(roll) + point.z * cos(roll)).toFloat(),
    )
    return Point3(
        x = (afterRoll.x * cos(pitch) + afterRoll.z * sin(pitch)).toFloat(),
        y = afterRoll.y,
        z = (-afterRoll.x * sin(pitch) + afterRoll.z * cos(pitch)).toFloat(),
    )
}

private fun project(point: Point3, center: Offset, scale: Float): ProjectedPoint {
    val cameraYaw = Math.toRadians(-32.0)
    val cameraPitch = Math.toRadians(24.0)
    val x1 = point.x * cos(cameraYaw) - point.y * sin(cameraYaw)
    val y1 = point.x * sin(cameraYaw) + point.y * cos(cameraYaw)
    val y2 = y1 * cos(cameraPitch) - point.z * sin(cameraPitch)
    val depth = y1 * sin(cameraPitch) + point.z * cos(cameraPitch)
    return ProjectedPoint(
        position = Offset(
            x = center.x + x1.toFloat() * scale,
            y = center.y - y2.toFloat() * scale,
        ),
        depth = depth.toFloat(),
    )
}

private fun formatAngle(value: Double): String =
    String.format(java.util.Locale.getDefault(), "%.1f", value)
