package com.ayni.mobile.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayni.mobile.R
import com.ayni.mobile.ui.theme.AyniBrandSoft
import com.ayni.mobile.ui.theme.AyniOnSurfaceVariant
import com.ayni.mobile.ui.theme.AyniPrimary
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniSurface
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TOTAL_DURATION_MS = 5000L
private const val TRACE_DURATION_MS = 900
private const val WORDMARK_DELAY_MS = 250L

/**
 * Primer momento de marca: reproduce el trazo tipo sismógrafo (eco de
 * `ic_launcher_foreground`, el elemento signature del triage estructural §6.3) y revela
 * "Ayni" + tagline. Dura ~5s en total o se salta tocando la pantalla — un guard
 * (`finished`) evita que auto-avance y tap disparen `onFinished` dos veces.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    val traceProgress = remember { Animatable(0f) }
    var wordmarkVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            traceProgress.animateTo(1f, animationSpec = tween(TRACE_DURATION_MS, easing = EaseOutCubic))
            wordmarkVisible = true
            delay(WORDMARK_DELAY_MS)
            taglineVisible = true
        }
        delay(TOTAL_DURATION_MS)
        finish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash-glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow-scale"
    )

    val wordmarkAlpha by animateFloatAsState(
        targetValue = if (wordmarkVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "wordmark-alpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "tagline-alpha"
    )

    val splashDescription = stringResource(R.string.splash_content_description)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AyniSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { finish() }
            )
            .semantics { contentDescription = splashDescription },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Resplandor ambiental pulsante detrás del trazo — sutil, no compite con
                // el semáforo de veredicto (§6.2, tokens intocables).
                Canvas(modifier = Modifier.size(180.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AyniBrandSoft.copy(alpha = 0.9f), AyniBrandSoft.copy(alpha = 0f))
                        ),
                        radius = (size.minDimension / 2f) * glowScale
                    )
                }
                Canvas(modifier = Modifier.size(width = 140.dp, height = 90.dp)) {
                    val w = size.width
                    val h = size.height
                    // Mismo trazo que ic_launcher_foreground, normalizado a este lienzo.
                    val points = listOf(
                        Offset(0.10f * w, 0.55f * h),
                        Offset(0.30f * w, 0.55f * h),
                        Offset(0.42f * w, 0.20f * h),
                        Offset(0.58f * w, 0.85f * h),
                        Offset(0.70f * w, 0.55f * h),
                        Offset(0.90f * w, 0.55f * h)
                    )
                    clipRect(right = w * traceProgress.value) {
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = AyniPrimaryContainer,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 7f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = AyniPrimary,
                modifier = Modifier
                    .padding(top = Spacing.lg)
                    .alpha(wordmarkAlpha)
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = AyniOnSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.sm, start = Spacing.xl, end = Spacing.xl)
                    .alpha(taglineAlpha)
            )
        }
    }
}
