package com.ayni.mobile.ui.iot.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayni.mobile.ui.theme.MetricNumberStyle

/**
 * Piezas compartidas por la sección de monitoreo IoT. Migradas de `core/ui/component/Common.kt`
 * de ProtoEstados para que las tarjetas del monitoreo se vean coherentes entre sí.
 */

/**
 * Fondo común de la sección. Los halos son estáticos para conservar batería y fluidez;
 * el movimiento se reserva para estados que sí comunican actividad.
 */
@Composable
fun PremiumBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val darkPalette = colors.background.luminance() < 0.5f
    val bottomTint = if (darkPalette) 0.18f else 0.10f
    val primaryGlow = if (darkPalette) 0.15f else 0.10f
    val secondaryGlow = if (darkPalette) 0.11f else 0.07f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surfaceContainerLowest,
                        colors.background,
                        lerp(colors.background, colors.secondaryContainer, bottomTint),
                    ),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topRadius = size.minDimension * 0.82f
            val bottomRadius = size.minDimension * 0.70f
            val topCenter = Offset(size.width * 0.88f, size.height * 0.04f)
            val bottomCenter = Offset(size.width * 0.02f, size.height * 0.82f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = primaryGlow),
                        Color.Transparent,
                    ),
                    center = topCenter,
                    radius = topRadius,
                ),
                radius = topRadius,
                center = topCenter,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.secondary.copy(alpha = secondaryGlow),
                        Color.Transparent,
                    ),
                    center = bottomCenter,
                    radius = bottomRadius,
                ),
                radius = bottomRadius,
                center = bottomCenter,
            )
        }
        content()
    }
}

/** Barra superior translúcida con una iluminación coherente en las secciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val darkPalette = colors.background.luminance() < 0.5f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colors.primary.copy(alpha = if (darkPalette) 0.16f else 0.12f),
                        colors.secondaryContainer.copy(
                            alpha = if (darkPalette) 0.38f else 0.26f,
                        ),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        TopAppBar(
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = colors.onSurface,
            ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            colors.primary.copy(alpha = 0.38f),
                            colors.secondary.copy(alpha = 0.24f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.35f)),
                    ),
                    RoundedCornerShape(999.dp),
                ),
        )
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun EmptyCard(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val colors = MaterialTheme.colorScheme
    val darkPalette = colors.background.luminance() < 0.5f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colors.outlineVariant.copy(
                    alpha = if (darkPalette) 0.88f else 0.65f,
                ),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerLow.copy(
                alpha = if (darkPalette) 0.98f else 0.82f,
            ),
        ),
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Fila etiqueta/valor. El valor va monoespaciado para que no se desplace
 * cuando cambia varias veces por segundo.
 */
@Composable
fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MetricNumberStyle)
    }
}

/**
 * Barra de nivel para telemetría en vivo.
 *
 * Un número que cambia se lee peor que una barra que se mueve: para comprobar
 * que un sensor responde, el movimiento es la señal.
 */
@Composable
fun LevelBar(
    value: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val fraction = if (maxValue > 0f) (value / maxValue).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 220),
        label = "level-bar",
    )
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(colors.surfaceVariant.copy(alpha = 0.7f), shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.55f), color),
                    ),
                    shape,
                ),
        )
    }
}

/** Dato principal de una tarjeta: un número grande y su etiqueta pequeña. */
@Composable
fun HeadlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

/** Etiqueta redondeada de estado. El color lo decide quien la usa. */
@Composable
fun StatusPill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    pulse: Boolean = false,
) {
    val darkPalette = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = container,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            content.copy(alpha = if (darkPalette) 0.30f else 0.18f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = content, pulse = pulse)
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
        }
    }
}

@Composable
private fun StatusDot(
    color: Color,
    pulse: Boolean,
) {
    val alpha = if (pulse) {
        val transition = rememberInfiniteTransition(label = "status-dot")
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.32f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "status-dot-alpha",
        )
        animatedAlpha
    } else {
        0.72f
    }
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(
                color.copy(alpha = alpha),
                RoundedCornerShape(999.dp),
            ),
    )
}

/**
 * Tarjeta base de una sección: título, subtítulo opcional y contenido.
 * Unifica las tres convenciones de tinte que convivían antes.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val darkPalette = colors.background.luminance() < 0.5f
    val resolvedAccent = accent ?: colors.secondary
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        resolvedAccent.copy(
                            alpha = if (darkPalette) 0.60f else 0.42f,
                        ),
                        colors.outlineVariant.copy(
                            alpha = if (darkPalette) 0.72f else 0.45f,
                        ),
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (accent == null) {
                colors.surfaceContainerLow.copy(
                    alpha = if (darkPalette) 0.98f else 0.92f,
                )
            } else {
                lerp(
                    if (darkPalette) {
                        colors.surfaceContainerLow
                    } else {
                        colors.surfaceContainerLowest
                    },
                    accent,
                    if (darkPalette) 0.085f else 0.055f,
                )
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (darkPalette) 4.dp else 2.dp,
        ),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                resolvedAccent.copy(
                                    alpha = if (darkPalette) 1f else 0.88f,
                                ),
                                resolvedAccent.copy(
                                    alpha = if (darkPalette) 0.28f else 0.16f,
                                ),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier.padding(
                    start = 17.dp,
                    top = 18.dp,
                    end = 17.dp,
                    bottom = 17.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    trailing?.invoke()
                }
                content()
            }
        }
    }
}
