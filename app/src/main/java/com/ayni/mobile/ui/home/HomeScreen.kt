package com.ayni.mobile.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayni.mobile.R
import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.ui.components.AiStatus
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.components.OfflineStatusBadge
import com.ayni.mobile.ui.proximity.hasProximityPermissions
import com.ayni.mobile.ui.proximity.requiredProximityPermissions
import com.ayni.mobile.ui.theme.AyniBrandSoft
import com.ayni.mobile.ui.theme.AyniDangerRed
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniInputBackground
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * F1: Home es el centro de **SOS**. El botón circular ya no marca al 911 — mantener 3s
 * activa/desactiva la baliza BLE del sistema de proximidad (mismo
 * ManageEmergencyProximityUseCase que usa la pantalla Proximidad, ver
 * HomeViewModel.onSosHoldComplete). Los accesos rápidos de abajo llevan a los dos módulos
 * reales de triage: Médico (MEDICAL_GRAPH) y Estructura (STRUCTURAL_GRAPH) — ya no hay
 * "Estoy Atrapado"/"Enviar Ubicación"/Contactos de Emergencia (eran UI sin backend).
 */
@Composable
fun HomeScreen(
    onDisclaimerClick: () -> Unit,
    onMedicalClick: () -> Unit,
    onStructuralClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onModelFileSelected) }

    var sosPermissionsGranted by remember { mutableStateOf(hasProximityPermissions(context)) }
    val sosPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { sosPermissionsGranted = hasProximityPermissions(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        val aiStatus = when {
            uiState.aiReady -> AiStatus.READY
            !uiState.modelFilePresent -> AiStatus.MODEL_MISSING
            else -> AiStatus.WARMING_UP
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Spacing.marginPage, vertical = Spacing.md)
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.marginPage)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(BottomNavClearance),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                TextButton(onClick = onDisclaimerClick) {
                    Text(stringResource(R.string.home_disclaimer_link))
                }

                ModelStatusBanner(
                    isImporting = uiState.isImportingModel,
                    modelPresent = uiState.modelFilePresent,
                    importError = uiState.importError,
                    onPickModel = { modelPickerLauncher.launch(arrayOf("*/*")) }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    OfflineStatusBadge(
                        aiStatus = aiStatus,
                        onPickModel = { modelPickerLauncher.launch(arrayOf("*/*")) }
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    SosButton(
                        status = uiState.sosStatus,
                        onHoldComplete = {
                            if (sosPermissionsGranted) {
                                viewModel.onSosHoldComplete()
                            } else {
                                sosPermissionLauncher.launch(requiredProximityPermissions())
                            }
                        }
                    )
                    Text(
                        text = stringResource(
                            when (uiState.sosStatus) {
                                SosModeStatus.ACTIVE, SosModeStatus.STARTING -> R.string.home_sos_hold_deactivate
                                else -> R.string.home_sos_hold_activate
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.lg)
            ) {
                SecondaryActionCard(
                    label = stringResource(R.string.home_action_medical),
                    icon = Icons.Filled.MedicalServices,
                    modifier = Modifier.weight(1f),
                    onClick = onMedicalClick
                )
                SecondaryActionCard(
                    label = stringResource(R.string.home_action_structural),
                    icon = Icons.Filled.CropFree,
                    modifier = Modifier.weight(1f),
                    onClick = onStructuralClick
                )
            }
        }
    }
}

@Composable
private fun ModelStatusBanner(
    isImporting: Boolean,
    modelPresent: Boolean,
    importError: Boolean,
    onPickModel: () -> Unit
) {
    if (isImporting) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            shape = AyniShapes.medium,
            color = AyniInputBackground
        ) {
            Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.home_model_importing),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
        }
    } else if (!modelPresent) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            shape = AyniShapes.medium,
            color = AyniInputBackground
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = if (importError) {
                        stringResource(R.string.home_model_import_error)
                    } else {
                        stringResource(R.string.home_model_missing_body)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onPickModel, modifier = Modifier.padding(top = Spacing.xs)) {
                    Text(stringResource(R.string.home_model_missing_action))
                }
            }
        }
    }
}

/**
 * Botón SOS circular. Mantener 3s dispara onHoldComplete, que en HomeViewModel alterna
 * activar/desactivar la baliza BLE según `status` (ver ManageEmergencyProximityUseCase).
 * El ícono es de transmisión (`Campaign`), no el pictograma "SOS" — refleja lo que el
 * botón realmente hace hoy (emitir una baliza Bluetooth), no una llamada de emergencia.
 * Con `status == ACTIVE` se ve un anillo tipo radar expandiéndose, para que "está
 * transmitiendo" se lea en el UI y no solo en el texto de abajo.
 */
@Composable
private fun SosButton(status: SosModeStatus, onHoldComplete: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var pressProgress by remember { mutableFloatStateOf(0f) }

    val radarTransition = rememberInfiniteTransition(label = "sos-radar")
    val radarProgress by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "radar-progress"
    )

    Box(
        modifier = Modifier
            .size(220.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val job = coroutineScope.launch {
                        val steps = 30
                        repeat(steps) { i ->
                            pressProgress = (i + 1f) / steps
                            kotlinx.coroutines.delay(3000L / steps)
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHoldComplete()
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    pressProgress = 0f
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = AyniBrandSoft)
            if (status == SosModeStatus.ACTIVE) {
                val maxRadius = size.minDimension / 2f
                drawCircle(
                    color = AyniDangerRed.copy(alpha = (1f - radarProgress) * 0.45f),
                    radius = maxRadius * (0.55f + radarProgress * 0.45f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                )
            }
            if (pressProgress > 0f) {
                drawArc(
                    color = AyniPrimaryContainer,
                    startAngle = -90f,
                    sweepAngle = 360f * pressProgress,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(172.dp)
                .background(AyniDangerRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = stringResource(R.string.home_sos_content_description),
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                val captionRes = when (status) {
                    SosModeStatus.STARTING -> R.string.home_sos_starting_caption
                    SosModeStatus.ACTIVE -> R.string.home_sos_active_caption
                    SosModeStatus.UNSUPPORTED -> R.string.home_sos_unsupported_caption
                    SosModeStatus.ERROR -> R.string.home_sos_error_caption
                    SosModeStatus.INACTIVE -> R.string.home_sos_inactive_caption
                }
                Text(
                    text = stringResource(captionRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (status == SosModeStatus.STARTING) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = Spacing.xs)
                            .size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.1f),
        shape = AyniShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, AyniHairline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AyniPrimaryFixed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AyniPrimaryContainer)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.sm, start = Spacing.xs, end = Spacing.xs)
            )
        }
    }
}
