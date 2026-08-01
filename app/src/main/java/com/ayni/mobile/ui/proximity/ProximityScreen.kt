package com.ayni.mobile.ui.proximity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayni.mobile.R
import com.ayni.mobile.domain.proximity.GattConfirmationStatus
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.ProximityRole
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.ProximitySignalLevel
import com.ayni.mobile.domain.proximity.ProximityTrend
import com.ayni.mobile.domain.proximity.RangingTechnology
import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.domain.proximity.UwbRangingStatus
import com.ayni.mobile.domain.proximity.proximityPulseCue
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.theme.AyniSemanticColors
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
fun ProximityRoute(
    onBack: () -> Unit,
    viewModel: ProximityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProximityScreen(
        state = state,
        onBack = { if (state.role == null) onBack() else viewModel.clearRole() },
        onSelectRole = viewModel::selectRole,
        onActivateSos = viewModel::activateSos,
        onDeactivateSos = viewModel::deactivateSos,
        onStartDetection = viewModel::startDetection,
        onStopDetection = viewModel::stopDetection,
        onSelectSignal = viewModel::selectSignal,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProximityScreen(
    state: ProximityUiState,
    onBack: () -> Unit,
    onSelectRole: (ProximityRole) -> Unit,
    onActivateSos: () -> Unit,
    onDeactivateSos: () -> Unit,
    onStartDetection: () -> Unit,
    onStopDetection: () -> Unit,
    onSelectSignal: (String) -> Unit,
) {
    val context = LocalContext.current
    var permissionsGranted by remember(state.role) {
        mutableStateOf(state.role?.let { hasRolePermissions(context, it) } ?: true)
    }
    var showSosConfirmation by remember { mutableStateOf(false) }
    var pulseEnabled by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { state.role?.let { permissionsGranted = hasRolePermissions(context, it) } }

    ProximityPulseEffect(
        signal = state.selectedSignal,
        distanceMeters = state.connection.distanceMeters.takeIf {
            state.connection.peerId == state.selectedPeerId
        },
        enabled = state.role == ProximityRole.RESCUER && pulseEnabled &&
            state.scanStatus == ProximityScanStatus.SCANNING && !state.signalLost,
    )

    if (showSosConfirmation) {
        AlertDialog(
            onDismissRequest = { showSosConfirmation = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.proximity_sos_confirm_title)) },
            text = { Text(stringResource(R.string.proximity_sos_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSosConfirmation = false
                    onActivateSos()
                }) { Text(stringResource(R.string.proximity_activate_sos)) }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmation = false }) {
                    Text(stringResource(R.string.proximity_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.role) {
                            ProximityRole.SOS -> stringResource(R.string.proximity_role_sos)
                            ProximityRole.RESCUER -> stringResource(R.string.proximity_role_rescuer)
                            null -> stringResource(R.string.proximity_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.role == null -> RoleChooser(
                modifier = Modifier.padding(padding),
                sosActive = state.sosStatus == SosModeStatus.ACTIVE,
                onSelectRole = onSelectRole,
            )
            !permissionsGranted -> PermissionRequired(
                modifier = Modifier.padding(padding),
                onRequest = { permissionLauncher.launch(requiredPermissions(context, state.role)) },
            )
            state.role == ProximityRole.SOS -> SosRoleContent(
                modifier = Modifier.padding(padding),
                state = state,
                onActivate = { showSosConfirmation = true },
                onDeactivate = onDeactivateSos,
            )
            else -> RescuerRoleContent(
                modifier = Modifier.padding(padding),
                state = state,
                pulseEnabled = pulseEnabled,
                onPulseEnabledChange = { pulseEnabled = it },
                onStart = onStartDetection,
                onStop = onStopDetection,
                onSelectSignal = onSelectSignal,
            )
        }
    }
}

@Composable
private fun RoleChooser(
    modifier: Modifier,
    sosActive: Boolean,
    onSelectRole: (ProximityRole) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(stringResource(R.string.proximity_choose_role), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.proximity_choose_role_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RoleCard(
            icon = { Icon(Icons.Filled.Campaign, null, tint = AyniSemanticColors.rojo) },
            title = stringResource(R.string.proximity_role_sos),
            body = stringResource(R.string.proximity_role_sos_body),
            emphasized = sosActive,
            onClick = { onSelectRole(ProximityRole.SOS) },
        )
        RoleCard(
            icon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
            title = stringResource(R.string.proximity_role_rescuer),
            body = stringResource(R.string.proximity_role_rescuer_body),
            emphasized = false,
            onClick = { onSelectRole(ProximityRole.RESCUER) },
        )
        Text(
            stringResource(R.string.proximity_safety_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RoleCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (emphasized) AyniSemanticColors.rojo.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { icon() }
            Column(Modifier.padding(start = Spacing.md)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SosRoleContent(
    modifier: Modifier,
    state: ProximityUiState,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { SosCard(state.sosStatus, onActivate, onDeactivate) }
        if (state.sosStatus == SosModeStatus.ACTIVE) {
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Confirmación de rescate", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.reception.confirmedDetectors == 0) {
                                stringResource(R.string.proximity_waiting_confirmation)
                            } else {
                                stringResource(R.string.proximity_confirmed_count, state.reception.confirmedDetectors)
                            },
                            color = if (state.reception.confirmedDetectors > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (state.reception.confirmedDetectors > 0) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            stringResource(
                                if (state.reception.uwbAvailable) R.string.proximity_uwb_ready
                                else R.string.proximity_uwb_fallback,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.proximity_safety_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SosCard(status: SosModeStatus, onActivate: () -> Unit, onDeactivate: () -> Unit) {
    val running = status == SosModeStatus.STARTING || status == SosModeStatus.ACTIVE
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (running) AyniSemanticColors.rojo.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Campaign, null, tint = AyniSemanticColors.rojo, modifier = Modifier.size(40.dp))
                Text(
                    stringResource(R.string.proximity_sos_mode),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            Text(sosStatusText(status), fontWeight = if (running) FontWeight.Bold else FontWeight.Normal)
            if (running) {
                PrimaryActionButton(
                    text = stringResource(R.string.proximity_stop_sos),
                    onClick = onDeactivate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            } else {
                PrimaryActionButton(
                    text = stringResource(R.string.proximity_activate_sos),
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AyniSemanticColors.rojo,
                        contentColor = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun RescuerRoleContent(
    modifier: Modifier,
    state: ProximityUiState,
    pulseEnabled: Boolean,
    onPulseEnabledChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSelectSignal: (String) -> Unit,
) {
    val scanning = state.scanStatus == ProximityScanStatus.SCANNING
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            DetectorControls(state.scanStatus, pulseEnabled, onPulseEnabledChange, onStart, onStop)
        }
        if (scanning) {
            item { ConnectionStatus(state) }
            item { SignalGuide(state) }
            if (state.signals.isNotEmpty()) {
                item { Text(stringResource(R.string.proximity_detected_signals), style = MaterialTheme.typography.titleMedium) }
                items(state.signals, key = { it.peerId }) { signal ->
                    SignalChoice(
                        signal = signal,
                        selected = signal.peerId == state.selectedPeerId,
                        onClick = { onSelectSignal(signal.peerId) },
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.proximity_safety_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetectorControls(
    status: ProximityScanStatus,
    pulseEnabled: Boolean,
    onPulseEnabledChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val scanning = status == ProximityScanStatus.SCANNING
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.proximity_detector_mode), style = MaterialTheme.typography.titleLarge)
            Text(scanStatusText(status), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (scanning) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(stringResource(R.string.proximity_pulse_toggle))
                    Switch(checked = pulseEnabled, onCheckedChange = onPulseEnabledChange)
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.proximity_stop_search))
                }
            } else {
                PrimaryActionButton(text = stringResource(R.string.proximity_start_search), onClick = onStart)
            }
        }
    }
}

@Composable
private fun ConnectionStatus(state: ProximityUiState) {
    if (state.selectedPeerId == null) return
    val text = when {
        state.connection.confirmationStatus == GattConfirmationStatus.CONNECTING ->
            stringResource(R.string.proximity_gatt_connecting)
        state.connection.confirmationStatus == GattConfirmationStatus.FAILED ->
            stringResource(R.string.proximity_gatt_failed)
        state.connection.uwbStatus == UwbRangingStatus.NEGOTIATING ->
            stringResource(R.string.proximity_uwb_negotiating)
        state.connection.technology == RangingTechnology.UWB ->
            stringResource(R.string.proximity_uwb_ranging)
        state.connection.confirmationStatus == GattConfirmationStatus.CONFIRMED ->
            stringResource(R.string.proximity_gatt_confirmed)
        else -> stringResource(R.string.proximity_ble_fallback)
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(Spacing.md), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SignalGuide(state: ProximityUiState) {
    val signal = state.selectedSignal
    val uwbDistance = state.connection.distanceMeters
        .takeIf { state.connection.peerId == state.selectedPeerId }
    val intensityTarget = when {
        uwbDistance != null -> (1f - (uwbDistance / 15f)).coerceIn(0.12f, 1f)
        signal?.level == ProximitySignalLevel.STRONG -> 1f
        signal?.level == ProximitySignalLevel.MEDIUM -> 0.62f
        signal?.level == ProximitySignalLevel.WEAK -> 0.28f
        else -> 0.08f
    }
    val intensity by animateFloatAsState(intensityTarget, label = "signal-intensity")
    val outlineColor = MaterialTheme.colorScheme.outline
    val description = when {
        state.signalLost -> stringResource(R.string.proximity_signal_lost)
        uwbDistance != null -> stringResource(R.string.proximity_distance_uwb, uwbDistance)
        signal != null -> "${signalLevelText(signal.level)}. ${trendText(signal.trend)}"
        else -> stringResource(R.string.proximity_searching_signal)
    }

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(230.dp).semantics { contentDescription = description }) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = min(size.width, size.height) / 2f
                    repeat(4) { index ->
                        drawCircle(
                            color = AyniSemanticColors.signal.copy(alpha = 0.12f + intensity * (0.07f + index * 0.025f)),
                            radius = maxRadius * ((index + 1) / 4f),
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx() + intensity * 3.dp.toPx()),
                        )
                    }
                    drawCircle(
                        color = if (signal == null && uwbDistance == null) outlineColor
                        else AyniSemanticColors.rojo,
                        radius = 12.dp.toPx() + intensity * 12.dp.toPx(),
                        center = center,
                    )
                }
                when {
                    state.signalLost -> Icon(Icons.Filled.Warning, null, tint = AyniSemanticColors.rojo, modifier = Modifier.size(48.dp))
                    state.connection.azimuthDegrees != null -> Icon(
                        Icons.Filled.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp).graphicsLayer { rotationZ = state.connection.azimuthDegrees ?: 0f },
                    )
                    signal == null -> CircularProgressIndicator()
                }
            }
            Text(
                when {
                    state.signalLost -> stringResource(R.string.proximity_signal_lost)
                    uwbDistance != null -> stringResource(R.string.proximity_distance_uwb, uwbDistance)
                    signal != null -> signalLevelText(signal.level)
                    else -> stringResource(R.string.proximity_searching_signal)
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                when {
                    state.signalLost -> stringResource(R.string.proximity_signal_lost_body)
                    state.connection.azimuthDegrees != null -> stringResource(R.string.proximity_azimuth_uwb, state.connection.azimuthDegrees ?: 0f)
                    signal != null -> trendText(signal.trend)
                    else -> stringResource(R.string.proximity_move_slowly)
                },
                color = if (state.signalLost) AyniSemanticColors.rojo else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            state.selectedPeerId?.let {
                Text(stringResource(R.string.proximity_signal_id, it.takeLast(6)), style = MaterialTheme.typography.labelMedium)
            }
            if (uwbDistance == null) signal?.let {
                Text(
                    stringResource(R.string.proximity_signal_technical, it.smoothedRssi.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (uwbDistance == null) stringResource(R.string.proximity_direction_warning)
                else stringResource(R.string.proximity_uwb_ranging),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignalChoice(signal: NearbySosSignal, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Campaign, null, tint = AyniSemanticColors.rojo)
            Column(Modifier.padding(start = Spacing.md)) {
                Text(stringResource(R.string.proximity_signal_id, signal.peerId.takeLast(6)), fontWeight = FontWeight.SemiBold)
                Text("${signalLevelText(signal.level)} · ${trendText(signal.trend)}")
            }
        }
    }
}

@Composable
private fun PermissionRequired(modifier: Modifier, onRequest: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.BluetoothSearching, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(R.string.proximity_permission_body), textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.lg))
        PrimaryActionButton(text = stringResource(R.string.proximity_permission_action), onClick = onRequest)
    }
}

@Composable
private fun sosStatusText(status: SosModeStatus): String = when (status) {
    SosModeStatus.INACTIVE -> stringResource(R.string.proximity_sos_inactive)
    SosModeStatus.STARTING -> stringResource(R.string.proximity_sos_starting)
    SosModeStatus.ACTIVE -> stringResource(R.string.proximity_sos_active)
    SosModeStatus.UNSUPPORTED -> stringResource(R.string.proximity_sos_unsupported)
    SosModeStatus.ERROR -> stringResource(R.string.proximity_sos_error)
}

@Composable
private fun scanStatusText(status: ProximityScanStatus): String = when (status) {
    ProximityScanStatus.IDLE -> stringResource(R.string.proximity_detector_idle)
    ProximityScanStatus.SCANNING -> stringResource(R.string.proximity_detector_scanning)
    ProximityScanStatus.BLUETOOTH_UNAVAILABLE -> stringResource(R.string.proximity_bluetooth_unavailable)
    ProximityScanStatus.PERMISSION_REQUIRED -> stringResource(R.string.proximity_permission_body)
    ProximityScanStatus.ERROR -> stringResource(R.string.proximity_scan_error)
}

@Composable
private fun signalLevelText(level: ProximitySignalLevel): String = when (level) {
    ProximitySignalLevel.WEAK -> stringResource(R.string.proximity_level_weak)
    ProximitySignalLevel.MEDIUM -> stringResource(R.string.proximity_level_medium)
    ProximitySignalLevel.STRONG -> stringResource(R.string.proximity_level_strong)
}

@Composable
private fun trendText(trend: ProximityTrend): String = when (trend) {
    ProximityTrend.APPROACHING -> stringResource(R.string.proximity_trend_approaching)
    ProximityTrend.STABLE -> stringResource(R.string.proximity_trend_stable)
    ProximityTrend.MOVING_AWAY -> stringResource(R.string.proximity_trend_away)
    ProximityTrend.UNKNOWN -> stringResource(R.string.proximity_trend_sampling)
}

@Composable
private fun ProximityPulseEffect(
    signal: NearbySosSignal?,
    distanceMeters: Float?,
    enabled: Boolean,
) {
    val haptic = LocalHapticFeedback.current
    val beepPlayer = remember { MediaProximityBeepPlayer() }
    val currentSignal by rememberUpdatedState(signal)
    val currentDistance by rememberUpdatedState(distanceMeters)
    DisposableEffect(beepPlayer) { onDispose(beepPlayer::release) }

    LaunchedEffect(signal?.peerId, enabled) {
        var pulseIndex = 0
        while (enabled) {
            val signalSnapshot = currentSignal ?: break
            val cue = proximityPulseCue(
                smoothedRssi = signalSnapshot.smoothedRssi,
                distanceMeters = currentDistance,
            )
            // Cerca, el audio puede llegar a 6-7 pulsos/s; se limita sólo la vibración
            // para no convertirla en un zumbido continuo.
            if (cue.closeness < 0.78 || pulseIndex % 3 == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            withContext(Dispatchers.Default) {
                beepPlayer.beep(cue.toneFrequencyHz, cue.toneDurationMillis)
            }
            pulseIndex++
            delay(cue.intervalMillis)
        }
    }
}

internal fun requiredPermissions(context: Context, role: ProximityRole): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_CONNECT)
        if (context.packageManager.hasSystemFeature("android.hardware.uwb")) {
            add(Manifest.permission.UWB_RANGING)
        }
        when (role) {
            ProximityRole.SOS -> add(Manifest.permission.BLUETOOTH_ADVERTISE)
            ProximityRole.RESCUER -> add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }
    if (role == ProximityRole.RESCUER) add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (role == ProximityRole.SOS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

internal fun hasRolePermissions(context: Context, role: ProximityRole): Boolean =
    requiredPermissions(context, role).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
