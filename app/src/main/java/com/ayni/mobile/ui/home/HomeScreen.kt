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
import androidx.compose.material.icons.filled.Sensors
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
import com.ayni.mobile.domain.proximity.GattConfirmationStatus
import com.ayni.mobile.domain.proximity.PeerConnectionState
import com.ayni.mobile.domain.proximity.ProximityRole
import com.ayni.mobile.domain.proximity.RangingTechnology
import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.domain.proximity.SosReceptionState
import com.ayni.mobile.ui.components.AiStatus
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.components.OfflineStatusBadge
import com.ayni.mobile.ui.proximity.hasRolePermissions
import com.ayni.mobile.ui.proximity.requiredPermissions
import com.ayni.mobile.ui.theme.AyniBrandSoft
import com.ayni.mobile.ui.theme.AyniDangerRed
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniInputBackground
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.AyniTertiary
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * F1: Home es el centro de señal de proximidad. El botón circular no marca al 911 ni dice
 * "SOS" en ningún estado — mantener 3s activa/desactiva la baliza BLE del sistema de
 * proximidad (mismo ManageEmergencyProximityUseCase que usa la pantalla Proximidad, ver
 * HomeViewModel.onSosHoldComplete) para que un rescatista pueda ubicarte. Mientras está
 * activa, refleja cercanía real (SosReceptionState/PeerConnectionState) con color y texto
 * — nunca una distancia inventada desde RSSI, ver SosButton. Los accesos rápidos de abajo
 * llevan a los dos módulos reales de triage: Médico (MEDICAL_GRAPH) y Estructura
 * (STRUCTURAL_GRAPH) — ya no hay "Estoy Atrapado"/"Enviar Ubicación"/Contactos de
 * Emergencia (eran UI sin backend).
 */
@Composable
fun HomeScreen(
    onMedicalClick: () -> Unit,
    onStructuralClick: () -> Unit,
    onMonitoringClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onModelFileSelected) }

    var sosPermissionsGranted by remember {
        mutableStateOf(hasRolePermissions(context, ProximityRole.SOS))
    }
    val sosPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { sosPermissionsGranted = hasRolePermissions(context, ProximityRole.SOS) }

    Column(modifier = Modifier.fillMaxSize()) {
        val aiStatus = when {
            uiState.aiReady -> AiStatus.READY
            uiState.isImportingModel -> AiStatus.IMPORTING
            !uiState.modelFilePresent -> AiStatus.MODEL_MISSING
            else -> AiStatus.WARMING_UP
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Spacing.marginPage, vertical = Spacing.md)
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.home_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                ModelStatusBanner(
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
                    Text(
                        text = stringResource(R.string.home_sos_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                    SosButton(
                        status = uiState.sosStatus,
                        reception = uiState.sosReceptionState,
                        connection = uiState.peerConnectionState,
                        onHoldComplete = {
                            if (sosPermissionsGranted) {
                                viewModel.onSosHoldComplete()
                            } else {
                                sosPermissionLauncher.launch(requiredPermissions(context, ProximityRole.SOS))
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
                SecondaryActionCard(
                    label = stringResource(R.string.home_action_iot),
                    icon = Icons.Filled.Sensors,
                    modifier = Modifier.weight(1f),
                    onClick = onMonitoringClick
                )
            }
        }
    }
}

@Composable
private fun ModelStatusBanner(
    importError: Boolean,
    onPickModel: () -> Unit
) {
    // Solo se muestra tras un intento de copia fallido. Los estados base ("falta el
    // modelo", "copiando modelo…") ya los cubre el único pill OfflineStatusBadge de más
    // abajo — tener un segundo panel con el mismo mensaje al mismo tiempo era redundante.
    if (importError) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            shape = AyniShapes.medium,
            color = AyniInputBackground
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = stringResource(R.string.home_model_import_error),
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
 * Botón de señal de proximidad. Mantener 3s dispara onHoldComplete, que en HomeViewModel
 * alterna activar/desactivar la baliza BLE (ver ManageEmergencyProximityUseCase). Ya no
 * dice "SOS" en ningún estado — el ícono es de transmisión (`Campaign`) y los textos
 * describen lo que el botón realmente hace: emitir una señal para que un rescatista te
 * ubique, no marcar una llamada de emergencia.
 *
 * Con `status == ACTIVE`, el color del núcleo y del anillo tipo radar se interpola entre
 * verde (lejos/sin contacto) y rojo (cerca) según datos reales de proximidad — nunca se
 * inventa una distancia en metros desde RSSI (regla de diseño): si hay ranging UWB
 * (`connection.distanceMeters`) se usa esa distancia real; si solo hay confirmación GATT
 * de que un rescatista te detectó (`reception.confirmedDetectors`), se usa un nivel fijo
 * "cerca" cualitativo; sin ninguna señal, el color se mantiene neutro.
 */
@Composable
private fun SosButton(
    status: SosModeStatus,
    reception: SosReceptionState,
    connection: PeerConnectionState,
    onHoldComplete: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var pressProgress by remember { mutableFloatStateOf(0f) }

    val uwbDistance = connection.distanceMeters.takeIf { connection.technology == RangingTechnology.UWB }
    val rescuerConfirmed = connection.confirmationStatus == GattConfirmationStatus.CONFIRMED ||
        reception.confirmedDetectors > 0
    val proximityTarget = when {
        uwbDistance != null -> (1f - (uwbDistance / 15f)).coerceIn(0.15f, 1f)
        rescuerConfirmed -> 0.55f
        else -> 0f
    }
    val proximity by androidx.compose.animation.core.animateFloatAsState(
        targetValue = proximityTarget,
        label = "proximity-intensity"
    )
    val proximityColor = androidx.compose.ui.graphics.lerp(AyniTertiary, AyniDangerRed, proximity)

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
                    color = proximityColor.copy(alpha = (1f - radarProgress) * 0.45f),
                    radius = maxRadius * (0.55f + radarProgress * 0.45f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                )
                if (proximity > 0f) {
                    repeat(3) { index ->
                        drawCircle(
                            color = proximityColor.copy(alpha = 0.10f + proximity * 0.12f),
                            radius = maxRadius * (0.62f + index * 0.13f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }
                }
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
                .background(if (status == SosModeStatus.ACTIVE) proximityColor else AyniDangerRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = stringResource(R.string.home_sos_content_description),
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = when {
                        status == SosModeStatus.STARTING -> stringResource(R.string.home_sos_starting_caption)
                        status == SosModeStatus.UNSUPPORTED -> stringResource(R.string.home_sos_unsupported_caption)
                        status == SosModeStatus.ERROR -> stringResource(R.string.home_sos_error_caption)
                        status == SosModeStatus.INACTIVE -> stringResource(R.string.home_sos_inactive_caption)
                        uwbDistance != null -> stringResource(R.string.home_sos_distance_caption, uwbDistance)
                        rescuerConfirmed -> stringResource(R.string.home_sos_rescuer_near_caption)
                        else -> stringResource(R.string.home_sos_active_caption)
                    },
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
