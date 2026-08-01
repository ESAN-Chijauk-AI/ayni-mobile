package com.ayni.mobile.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * §7 del spec: disclaimer claro y visible (onboarding obligatorio la primera vez +
 * accesible desde resultado) — requiere confirmación explícita, no se auto-salta (a
 * diferencia del SplashScreen previo). Un solo texto fuente de verdad para ambos casos.
 *
 * Rediseño: en vez de 3 párrafos seguidos (muro de texto), cada punto es una tarjeta con
 * su propio ícono + entrada escalonada — misma información, mejor jerarquía visual.
 */
@Composable
fun DisclaimerScreen(
    onContinue: () -> Unit,
    viewModel: DisclaimerViewModel = hiltViewModel()
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StaggeredReveal(index = 0) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AyniPrimaryFixed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Shield, contentDescription = null, tint = AyniPrimaryContainer)
                }
            }
            StaggeredReveal(index = 1) {
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            StaggeredReveal(index = 2) {
                DisclaimerPoint(icon = Icons.Filled.Info, text = stringResource(R.string.disclaimer_body_purpose))
            }
            StaggeredReveal(index = 3) {
                DisclaimerPoint(icon = Icons.Filled.Lock, text = stringResource(R.string.disclaimer_body_privacy))
            }
            StaggeredReveal(index = 4) {
                DisclaimerPoint(icon = Icons.Filled.WarningAmber, text = stringResource(R.string.disclaimer_body_caution))
            }

            StaggeredReveal(index = 5) {
                PrimaryActionButton(
                    text = stringResource(R.string.disclaimer_action_acknowledge),
                    onClick = {
                        viewModel.onAcknowledge()
                        onContinue()
                    }
                )
            }
        }
    }
}

@Composable
private fun DisclaimerPoint(icon: ImageVector, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AyniShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, AyniHairline)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AyniPrimaryFixed.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AyniPrimaryContainer)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = Spacing.md)
            )
        }
    }
}

/** Entrada escalonada sutil (fade + slide-up) para que el disclaimer no aparezca de golpe. */
@Composable
private fun StaggeredReveal(index: Int, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L * index)
        progress.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 20.dp.toPx()
        }
    ) {
        content()
    }
}
