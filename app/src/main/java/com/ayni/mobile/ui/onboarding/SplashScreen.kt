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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayni.mobile.R
import com.ayni.mobile.ui.theme.AyniOnSurfaceVariant
import com.ayni.mobile.ui.theme.AyniPrimary
import com.ayni.mobile.ui.theme.AyniSurface
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TOTAL_DURATION_MS = 5000L
private const val TRACE_DURATION_MS = 900
private const val WORDMARK_DELAY_MS = 250L

/**
 * Primer momento de marca: revela el logo real de Ayni (`ic_launcher_background`, el
 * mismo PNG del ícono de launcher — edificio agrietado + trazo sismógrafo + halo ámbar,
 * ya trae su propio resplandor) con un fade+escala de entrada, luego "Ayni" + tagline.
 * Dura ~5s en total o se salta tocando la pantalla — un guard (`finished`) evita que
 * auto-avance y tap disparen `onFinished` dos veces.
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

    val logoReveal = remember { Animatable(0f) }
    var wordmarkVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            logoReveal.animateTo(1f, animationSpec = tween(TRACE_DURATION_MS, easing = EaseOutCubic))
            wordmarkVisible = true
            delay(WORDMARK_DELAY_MS)
            taglineVisible = true
        }
        delay(TOTAL_DURATION_MS)
        finish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash-glow")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathing-scale"
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
            val entryScale = 0.75f + logoReveal.value * 0.25f
            Image(
                painter = painterResource(R.drawable.ic_launcher_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .graphicsLayer {
                        scaleX = entryScale * breathingScale
                        scaleY = entryScale * breathingScale
                        alpha = logoReveal.value
                    }
            )

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
