package com.ayni.mobile.ui.proximity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.ProximitySignalLevel
import com.ayni.mobile.domain.proximity.ProximityTrend
import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.theme.AyniSemanticColors
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun ProximityRoute(
    onBack: () -> Unit,
    viewModel: ProximityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProximityScreen(
        state = state,
        onBack = onBack,
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
    onActivateSos: () -> Unit,
    onDeactivateSos: () -> Unit,
    onStartDetection: () -> Unit,
    onStopDetection: () -> Unit,
    onSelectSignal: (String) -> Unit,
) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(hasProximityPermissions(context)) }
    var showSosConfirmation by remember { mutableStateOf(false) }
    var pulseEnabled by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionsGranted = hasProximityPermissions(context) }

    ProximityPulseEffect(
        signal = state.selectedSignal,
        enabled = pulseEnabled && state.scanStatus == ProximityScanStatus.SCANNING,
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
                title = { Text(stringResource(R.string.proximity_title)) },
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
        if (!permissionsGranted) {
            PermissionRequired(
                modifier = Modifier.padding(padding),
                onRequest = { permissionLauncher.launch(requiredProximityPermissions()) },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.proximity_safety_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    SosCard(
                        status = state.sosStatus,
                        detectionActive = state.scanStatus == ProximityScanStatus.SCANNING,
                        onActivate = { showSosConfirmation = true },
                        onDeactivate = onDeactivateSos,
                    )
                }
                item {
                    DetectorControls(
                        state = state,
                        pulseEnabled = pulseEnabled,
                        onPulseEnabledChange = { pulseEnabled = it },
                        onStart = onStartDetection,
                        onStop = onStopDetection,
                    )
                }
                if (state.scanStatus == ProximityScanStatus.SCANNING) {
                    item {
                        SignalGuide(
                            selectedPeerId = state.selectedPeerId,
                            signal = state.selectedSignal,
                        )
                    }
                    if (state.signals.size > 1) {
                        item {
                            Text(
                                stringResource(R.string.proximity_detected_signals),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(state.signals, key = { it.peerId }) { signal ->
                            SignalChoice(
                                signal = signal,
                                selected = signal.peerId == state.selectedPeerId,
                                onClick = { onSelectSignal(signal.peerId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequired(modifier: Modifier, onRequest: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.proximity_permission_body),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        PrimaryActionButton(
            text = stringResource(R.string.proximity_permission_action),
            onClick = onRequest,
        )
    }
}

@Composable
private fun SosCard(
    status: SosModeStatus,
    detectionActive: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
) {
    val active = status != SosModeStatus.INACTIVE
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (active) {
                AyniSemanticColors.rojo.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = AyniSemanticColors.rojo,
                )
                Text(
                    stringResource(R.string.proximity_sos_mode),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
            Text(
                when (status) {
                    SosModeStatus.INACTIVE -> stringResource(R.string.proximity_sos_inactive)
                    SosModeStatus.STARTING -> stringResource(R.string.proximity_sos_starting)
                    SosModeStatus.ACTIVE -> stringResource(R.string.proximity_sos_active)
                    SosModeStatus.UNSUPPORTED -> stringResource(R.string.proximity_sos_unsupported)
                    SosModeStatus.ERROR -> stringResource(R.string.proximity_sos_error)
                },
                color = if (status == SosModeStatus.ACTIVE) {
                    AyniSemanticColors.rojo
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (status == SosModeStatus.ACTIVE) FontWeight.Bold else FontWeight.Normal,
            )
            if (active) {
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
                    enabled = !detectionActive,
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
private fun DetectorControls(
    state: ProximityUiState,
    pulseEnabled: Boolean,
    onPulseEnabledChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val scanning = state.scanStatus == ProximityScanStatus.SCANNING
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(R.string.proximity_detector_mode),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                scanStatusText(state.scanStatus),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (scanning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.proximity_pulse_toggle))
                    Switch(checked = pulseEnabled, onCheckedChange = onPulseEnabledChange)
                }
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.proximity_stop_search))
                }
            } else {
                PrimaryActionButton(
                    text = stringResource(R.string.proximity_start_search),
                    onClick = onStart,
                    enabled = state.sosStatus == SosModeStatus.INACTIVE,
                )
            }
        }
    }
}

@Composable
private fun SignalGuide(selectedPeerId: String?, signal: NearbySosSignal?) {
    val intensityTarget = when (signal?.level) {
        ProximitySignalLevel.STRONG -> 1f
        ProximitySignalLevel.MEDIUM -> 0.62f
        ProximitySignalLevel.WEAK -> 0.28f
        null -> 0.08f
    }
    val intensity by animateFloatAsState(intensityTarget, label = "signal-intensity")
    val outlineColor = MaterialTheme.colorScheme.outline
    val description = signal?.let {
        "${signalLevelText(it.level)}. ${trendText(it.trend)}"
    } ?: stringResource(R.string.proximity_searching_signal)

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .size(230.dp)
                        .semantics { contentDescription = description },
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = min(size.width, size.height) / 2f
                    repeat(4) { index ->
                        val fraction = (index + 1) / 4f
                        drawCircle(
                            color = AyniSemanticColors.signal.copy(
                                alpha = 0.12f + intensity * (0.07f + index * 0.025f),
                            ),
                            radius = maxRadius * fraction,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx() + intensity * 3.dp.toPx(),
                            ),
                        )
                    }
                    drawCircle(
                        color = if (signal == null) {
                            outlineColor
                        } else {
                            AyniSemanticColors.rojo
                        },
                        radius = 12.dp.toPx() + intensity * 12.dp.toPx(),
                        center = center,
                    )
                }
                if (signal == null) CircularProgressIndicator()
            }
            Text(
                text = signal?.let { signalLevelText(it.level) }
                    ?: stringResource(R.string.proximity_searching_signal),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = signal?.let { trendText(it.trend) }
                    ?: stringResource(R.string.proximity_move_slowly),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (selectedPeerId != null) {
                Text(
                    stringResource(R.string.proximity_signal_id, selectedPeerId.takeLast(6)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            signal?.let {
                Text(
                    stringResource(R.string.proximity_signal_technical, it.smoothedRssi.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.proximity_direction_warning),
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Campaign, contentDescription = null, tint = AyniSemanticColors.rojo)
            Column(modifier = Modifier.padding(start = Spacing.md)) {
                Text(
                    stringResource(R.string.proximity_signal_id, signal.peerId.takeLast(6)),
                    fontWeight = FontWeight.SemiBold,
                )
                Text("${signalLevelText(signal.level)} · ${trendText(signal.trend)}")
            }
        }
    }
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
private fun ProximityPulseEffect(signal: NearbySosSignal?, enabled: Boolean) {
    val haptic = LocalHapticFeedback.current
    val tone = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65) }.getOrNull()
    }
    DisposableEffect(tone) { onDispose { tone?.release() } }

    LaunchedEffect(signal?.peerId, signal?.level, enabled) {
        while (enabled && signal != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
            delay(
                when (signal.level) {
                    ProximitySignalLevel.WEAK -> 2_500L
                    ProximitySignalLevel.MEDIUM -> 1_250L
                    ProximitySignalLevel.STRONG -> 550L
                },
            )
        }
    }
}

private fun requiredProximityPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

private fun hasProximityPermissions(context: Context): Boolean =
    requiredProximityPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
