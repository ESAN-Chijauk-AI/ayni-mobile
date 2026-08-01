package com.ayni.mobile.ui.iot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayni.mobile.R
import com.ayni.mobile.data.iot.local.HitMeasurementEntity
import com.ayni.mobile.data.iot.local.ImuDeviceEntity
import com.ayni.mobile.data.iot.local.MeasurementTraceEntity
import com.ayni.mobile.data.iot.local.RegisteredStateEntity
import com.ayni.mobile.data.iot.local.SeismicAnalysisEntity
import com.ayni.mobile.data.iot.local.SeismicEventEntity
import com.ayni.mobile.data.iot.local.SensorInstallationEntity
import com.ayni.mobile.data.iot.local.StructureEntity
import com.ayni.mobile.domain.iot.DiscoveredNode
import com.ayni.mobile.domain.iot.LinkState
import com.ayni.mobile.domain.iot.MeasurementGuidanceState
import com.ayni.mobile.domain.iot.MeasurementMode
import com.ayni.mobile.domain.iot.NodeWifiStatus
import com.ayni.mobile.domain.iot.OperationalPhase
import com.ayni.mobile.domain.iot.StructuralSnapshot
import com.ayni.mobile.domain.iot.angle
import com.ayni.mobile.domain.iot.angleDelta
import com.ayni.mobile.domain.iot.dateTime
import com.ayni.mobile.domain.iot.decimal
import com.ayni.mobile.domain.iot.hz
import com.ayni.mobile.domain.iot.isMeasurementStateAfterEvent
import com.ayni.mobile.domain.iot.isMeasurementStateBeforeEvent
import com.ayni.mobile.domain.iot.measurementGuidanceState
import com.ayni.mobile.domain.iot.sameInstallationContext
import com.ayni.mobile.domain.iot.shortDeviceId
import com.ayni.mobile.domain.iot.summarizeDiagnostics
import com.ayni.mobile.domain.iot.supportsLiveRestTelemetry
import com.ayni.mobile.domain.iot.signed
import com.ayni.mobile.domain.model.StructuralVerdict
import com.ayni.mobile.ui.iot.components.DiagnosticTallyCard
import com.ayni.mobile.ui.iot.components.EmptyCard
import com.ayni.mobile.ui.iot.components.LevelBar
import com.ayni.mobile.ui.iot.components.MeasurementTraceCard
import com.ayni.mobile.ui.iot.components.MeasurementTraceDetails
import com.ayni.mobile.ui.iot.components.MetricRow
import com.ayni.mobile.ui.iot.components.PremiumBackdrop
import com.ayni.mobile.ui.iot.components.PremiumTopAppBar
import com.ayni.mobile.ui.iot.components.SectionCard
import com.ayni.mobile.ui.iot.components.SectionTitle
import com.ayni.mobile.ui.iot.components.SensorOrientationView
import com.ayni.mobile.ui.iot.components.StatusPill
import kotlin.math.abs

private val REQUIRED_BLE_PERMISSIONS = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
)

private fun hasBlePermissions(context: Context): Boolean =
    REQUIRED_BLE_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Punto de entrada del destino IoT: engancha el [MonitoringViewModel] de Hilt, recolecta
 * sus flujos y gestiona el permiso de Bluetooth cercano antes de escanear. Reemplaza el
 * cableado de 30 callbacks que hacía `MainActivity` en ProtoEstados.
 */
@Composable
fun MonitoringRoute(
    viewModel: MonitoringViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val candidateHits = viewModel.candidateHits.collectAsStateWithLifecycle().value
    val measurementTraces = viewModel.measurementTraces.collectAsStateWithLifecycle().value
    val rememberedDevices = viewModel.rememberedDevices.collectAsStateWithLifecycle().value
    val structures = viewModel.structures.collectAsStateWithLifecycle().value
    val installations = viewModel.installations.collectAsStateWithLifecycle().value
    val activeInstallation = viewModel.activeInstallation.collectAsStateWithLifecycle().value
    val registeredStates = viewModel.registeredStates.collectAsStateWithLifecycle().value
    val seismicEvents = viewModel.seismicEvents.collectAsStateWithLifecycle().value
    val seismicAnalyses = viewModel.seismicAnalyses.collectAsStateWithLifecycle().value

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = REQUIRED_BLE_PERMISSIONS.all { result[it] == true }
        viewModel.setPermissionsGranted(granted)
        if (granted) viewModel.startScan()
    }

    LaunchedEffect(Unit) {
        viewModel.setPermissionsGranted(hasBlePermissions(context))
    }

    PremiumBackdrop {
        MonitoringScreen(
            state = state,
            candidateHits = candidateHits,
            measurementTraces = measurementTraces,
            rememberedDevices = rememberedDevices,
            structures = structures,
            installations = installations,
            activeInstallation = activeInstallation,
            registeredStates = registeredStates,
            seismicEvents = seismicEvents,
            seismicAnalyses = seismicAnalyses,
            onSearch = {
                if (hasBlePermissions(context)) {
                    viewModel.setPermissionsGranted(true)
                    viewModel.startScan()
                } else {
                    permissionLauncher.launch(REQUIRED_BLE_PERMISSIONS)
                }
            },
            onConnect = viewModel::connect,
            onDisconnect = viewModel::disconnect,
            onRefresh = viewModel::refresh,
            onReset = viewModel::resetAlgorithm,
            onCancelAttempt = viewModel::cancelAttempt,
            onHitsEnabled = viewModel::setHitsEnabled,
            onSeismicEnabled = viewModel::setSeismicEnabled,
            onToggleHit = viewModel::toggleCandidateHit,
            onSelectAllHits = viewModel::selectAllValidHits,
            onClearHitSelection = viewModel::clearHitSelection,
            onAnalyzeLatestHits = viewModel::analyzeLatestValidHits,
            onRegisterState = viewModel::registerSelectedState,
            onStartInstallation = viewModel::startInstallation,
            onDeleteHit = viewModel::deleteHit,
            onDeleteStructureHistory = viewModel::deleteStructureHistory,
            onClearAllTestHistory = viewModel::clearAllTestHistory,
            onSelectSensor = viewModel::selectSensor,
            onForgetSensor = viewModel::forgetSensor,
            onToggleComparison = viewModel::toggleStateComparison,
            onClearComparison = viewModel::clearStateComparison,
            onSelectSeismicEvent = viewModel::selectSeismicEvent,
            onSelectBeforeState = viewModel::selectBeforeState,
            onSelectAfterState = viewModel::selectAfterState,
            onSaveSeismicAnalysis = viewModel::saveSeismicAnalysis,
            onSetWifi = viewModel::setNodeWifi,
            onDismissError = viewModel::dismissError,
            onDismissMessage = viewModel::dismissMessage,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Sección de monitoreo estructural: todo lo relativo a los nodos ESP32.
 *
 * Tres sub-pestañas. Migrado de ProtoEstados sin cambios de comportamiento.
 */
@Composable
fun MonitoringScreen(
    state: IotUiState,
    candidateHits: List<HitMeasurementEntity>,
    measurementTraces: List<MeasurementTraceEntity>,
    rememberedDevices: List<ImuDeviceEntity>,
    structures: List<StructureEntity>,
    installations: List<SensorInstallationEntity>,
    activeInstallation: SensorInstallationEntity?,
    registeredStates: List<RegisteredStateEntity>,
    seismicEvents: List<SeismicEventEntity>,
    seismicAnalyses: List<SeismicAnalysisEntity>,
    onSearch: () -> Unit,
    onConnect: (DiscoveredNode) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
    onCancelAttempt: () -> Unit,
    onHitsEnabled: (Boolean) -> Unit,
    onSeismicEnabled: (Boolean) -> Unit,
    onToggleHit: (HitMeasurementEntity) -> Unit,
    onSelectAllHits: () -> Unit,
    onClearHitSelection: () -> Unit,
    onAnalyzeLatestHits: () -> Unit,
    onRegisterState: (String) -> Unit,
    onStartInstallation: (String?, String, String, String, String) -> Unit,
    onDeleteHit: (HitMeasurementEntity) -> Unit,
    onDeleteStructureHistory: (StructureEntity) -> Unit,
    onClearAllTestHistory: () -> Unit,
    onSelectSensor: (ImuDeviceEntity) -> Unit,
    onForgetSensor: (ImuDeviceEntity) -> Unit,
    onToggleComparison: (RegisteredStateEntity) -> Unit,
    onClearComparison: () -> Unit,
    onSelectSeismicEvent: (String) -> Unit,
    onSelectBeforeState: (String) -> Unit,
    onSelectAfterState: (String) -> Unit,
    onSaveSeismicAnalysis: () -> Unit,
    onSetWifi: (String, String) -> Unit,
    onDismissError: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val snackbarHost = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val darkPalette = MaterialTheme.colorScheme.background.luminance() < 0.5f

    state.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHost.showSnackbar(error)
            onDismissError()
        }
    }
    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHost.showSnackbar(message)
            onDismissMessage()
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Reiniciar el algoritmo?") },
            text = {
                Text(
                    "El ESP32 descartará la captura en curso y volverá a calibrarse. " +
                        "Los golpes y estados ya guardados en el celular no se borran.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onReset()
                    },
                ) { Text("Reiniciar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancelar") }
            },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopAppBar(
                title = stringResource(R.string.nav_monitoring),
                subtitle = state.linkMessage,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val tabLabels = listOf(
                stringResource(R.string.monitoring_tab_measure),
                stringResource(R.string.monitoring_tab_history),
                stringResource(R.string.monitoring_tab_equipment),
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                color = (
                    if (darkPalette) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    ).copy(alpha = if (darkPalette) 0.98f else 0.90f),
                tonalElevation = 3.dp,
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        Tab(
                            selected = selected,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    label,
                                    fontWeight = if (selected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                        )
                    }
                }
            }
            when (selectedTab) {
                0 -> MeasurementTab(
                    state = state,
                    candidateHits = candidateHits,
                    measurementTraces = measurementTraces,
                    structures = structures,
                    activeInstallation = activeInstallation,
                    onSearch = onSearch,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onRefresh = onRefresh,
                    onReset = { confirmReset = true },
                    onCancelAttempt = onCancelAttempt,
                    onHitsEnabled = onHitsEnabled,
                    onSeismicEnabled = onSeismicEnabled,
                    onToggleHit = onToggleHit,
                    onSelectAllHits = onSelectAllHits,
                    onClearHitSelection = onClearHitSelection,
                    onAnalyzeLatestHits = onAnalyzeLatestHits,
                    onRegisterState = onRegisterState,
                    onStartInstallation = onStartInstallation,
                    onDeleteHit = onDeleteHit,
                )

                1 -> HistoryTab(
                    state = state,
                    registeredStates = registeredStates,
                    seismicEvents = seismicEvents,
                    seismicAnalyses = seismicAnalyses,
                    structures = structures,
                    installations = installations,
                    onToggleComparison = onToggleComparison,
                    onClearComparison = onClearComparison,
                    onSelectSeismicEvent = onSelectSeismicEvent,
                    onSelectBeforeState = onSelectBeforeState,
                    onSelectAfterState = onSelectAfterState,
                    onSaveSeismicAnalysis = onSaveSeismicAnalysis,
                )

                else -> SensorsTab(
                    devices = rememberedDevices,
                    connectedDeviceId = state.operationalStatus?.deviceId,
                    selectedDeviceId = state.selectedDeviceId,
                    structures = structures,
                    installations = installations,
                    activeInstallationId = activeInstallation?.installationId,
                    onSelectSensor = onSelectSensor,
                    onForgetSensor = onForgetSensor,
                    onDeleteStructureHistory = onDeleteStructureHistory,
                    onClearAllTestHistory = onClearAllTestHistory,
                    wifi = state.operationalStatus?.wifi,
                    wifiEditable = state.linkState == LinkState.READY,
                    onSetWifi = onSetWifi,
                )
            }
        }
    }
}

@Composable
private fun MeasurementTab(
    state: IotUiState,
    candidateHits: List<HitMeasurementEntity>,
    measurementTraces: List<MeasurementTraceEntity>,
    structures: List<StructureEntity>,
    activeInstallation: SensorInstallationEntity?,
    onSearch: () -> Unit,
    onConnect: (DiscoveredNode) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
    onCancelAttempt: () -> Unit,
    onHitsEnabled: (Boolean) -> Unit,
    onSeismicEnabled: (Boolean) -> Unit,
    onToggleHit: (HitMeasurementEntity) -> Unit,
    onSelectAllHits: () -> Unit,
    onClearHitSelection: () -> Unit,
    onAnalyzeLatestHits: () -> Unit,
    onRegisterState: (String) -> Unit,
    onStartInstallation: (String?, String, String, String, String) -> Unit,
    onDeleteHit: (HitMeasurementEntity) -> Unit,
) {
    var stateName by rememberSaveable { mutableStateOf("") }
    var showStructureDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteHit by remember { mutableStateOf<HitMeasurementEntity?>(null) }
    val isReady = state.linkState == LinkState.READY
    val activeStructure = structures.firstOrNull {
        it.structureId == activeInstallation?.structureId
    }
    val validHits = candidateHits.count { it.valid }
    val tracesByKey = remember(measurementTraces) {
        measurementTraces.associateBy { it.stableKey() }
    }

    if (showStructureDialog) {
        StructureSetupDialog(
            structures = structures,
            onDismiss = { showStructureDialog = false },
            onConfirm = { structureId, name, description, surface, location ->
                showStructureDialog = false
                onStartInstallation(
                    structureId,
                    name,
                    description,
                    surface,
                    location,
                )
            },
        )
    }
    pendingDeleteHit?.let { hit ->
        AlertDialog(
            onDismissRequest = { pendingDeleteHit = null },
            title = { Text("¿Eliminar golpe #${hit.sequence}?") },
            text = {
                Text(
                    "Se eliminarán el golpe y su captura de 100 Hz. Si el golpe era " +
                        "válido y pertenece al montaje activo, el ESP32 volverá a calibrarse.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteHit = null
                        onDeleteHit(hit)
                    },
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteHit = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ConnectionCard(
                state = state,
                onSearch = onSearch,
                onDisconnect = onDisconnect,
                onRefresh = onRefresh,
            )
        }

        item {
            ActiveStructureCard(
                structure = activeStructure,
                installation = activeInstallation,
                canChange = isReady && state.selectedDeviceId != null,
                onChange = { showStructureDialog = true },
            )
        }

        if (!isReady && state.nodes.isNotEmpty()) {
            item { SectionTitle("Nodos encontrados") }
            items(state.nodes, key = { it.address }) { node ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConnect(node) },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.name, fontWeight = FontWeight.SemiBold)
                            Text(node.address, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${node.rssi} dBm")
                    }
                }
            }
        }

        item {
            ModeCard(
                mode = state.measurementMode,
                enabled = isReady && activeInstallation != null,
                onHitsEnabled = onHitsEnabled,
                onSeismicEnabled = onSeismicEnabled,
            )
        }

        item {
            if (activeInstallation == null) {
                EmptyCard(
                    "Selecciona o crea una estructura. La app reiniciará y calibrará " +
                        "el ESP32 antes de habilitar Golpes o Sísmico.",
                )
            } else {
                GuidanceCard(
                    mode = state.measurementMode,
                    phase = state.operationalStatus?.phase ?: OperationalPhase.UNKNOWN,
                    progress = state.operationalStatus?.progress ?: 0,
                )
            }
        }

        state.currentSnapshot?.let { snapshot ->
            item {
                SectionCard(
                    title = stringResource(R.string.monitoring_orientation_title),
                    subtitle = stringResource(R.string.monitoring_orientation_subtitle),
                    accent = MaterialTheme.colorScheme.secondary,
                ) {
                    SensorOrientationView(
                        rollDeg = snapshot.currentRollDeg,
                        pitchDeg = snapshot.currentPitchDeg,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                LiveMetrics(
                    snapshot = snapshot,
                    mode = state.measurementMode,
                    liveTelemetry = supportsLiveRestTelemetry(
                        state.operationalStatus?.firmwareVersion.orEmpty(),
                    ),
                )
            }
        }

        measurementTraces.firstOrNull()?.let { trace ->
            item(key = "latest-trace-${trace.stableKey()}") {
                var expanded by rememberSaveable(trace.stableKey()) {
                    mutableStateOf(true)
                }
                SectionTitle("Última captura · 100 lecturas por segundo")
                MeasurementTraceCard(
                    trace = trace,
                    expanded = expanded,
                    onToggleExpanded = { expanded = !expanded },
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                ) { Text("Reiniciar") }
                OutlinedButton(
                    onClick = onCancelAttempt,
                    enabled = isReady && state.measurementMode == MeasurementMode.HITS,
                    modifier = Modifier.weight(1f),
                ) { Text("Cancelar golpe") }
            }
        }

        item {
            // El código de diagnóstico vive en la traza, no en el golpe: se cruzan
            // por su clave. Una captura sin traza cuenta como desconocida en vez
            // de desaparecer del recuento.
            DiagnosticTallyCard(
                tally = remember(candidateHits, tracesByKey) {
                    summarizeDiagnostics(
                        candidateHits.map { hit ->
                            tracesByKey[hit.stableKey()]?.rejectionCode
                        },
                    )
                },
            )
        }

        item {
            Button(
                onClick = onAnalyzeLatestHits,
                enabled = activeInstallation != null &&
                    state.structuralSafety !is StructuralSafetyUiState.Working,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.structuralSafety is StructuralSafetyUiState.Working) {
                        "Gemma está analizando..."
                    } else {
                        "Analizar últimos golpes válidos con Gemma"
                    },
                )
            }
        }

        when (val safety = state.structuralSafety) {
            StructuralSafetyUiState.Idle -> Unit

            StructuralSafetyUiState.Working -> item {
                SectionCard(
                    title = "Analizando el espacio",
                    subtitle = "Espera mientras Gemma revisa los golpes recientes.",
                    accent = MaterialTheme.colorScheme.primary,
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            is StructuralSafetyUiState.InsufficientData -> item {
                EmptyCard(
                    "Hay ${safety.validHitCount} de al menos ${safety.requiredCount} " +
                        "golpes válidos. Registra más golpes y vuelve a analizar.",
                )
            }

            is StructuralSafetyUiState.Failure -> item {
                SectionCard(
                    title = "No se completó el análisis",
                    subtitle = safety.message,
                    accent = MaterialTheme.colorScheme.error,
                ) {}
            }

            is StructuralSafetyUiState.Success -> item {
                val accent = when (safety.analysis.verdict) {
                    StructuralVerdict.VERDE -> MaterialTheme.colorScheme.primary
                    StructuralVerdict.AMARILLO -> MaterialTheme.colorScheme.tertiary
                    StructuralVerdict.ROJO -> MaterialTheme.colorScheme.error
                }
                SectionCard(
                    title = safety.analysis.summary,
                    subtitle = "${safety.validHitCount} golpes válidos analizados · " +
                        "máximo ${safety.maximumHitCount}",
                    accent = accent,
                ) {
                    safety.analysis.steps.forEachIndexed { index, step ->
                        Text("${index + 1}. $step")
                    }
                }
            }
        }

        item {
            SectionTitle("Golpes candidatos")
            val selectedDeviceId = state.selectedDeviceId
            Text(
                if (selectedDeviceId == null) {
                    "Ningún sensor seleccionado"
                } else {
                    val displayName = state.operationalStatus
                        ?.takeIf { it.deviceId == selectedDeviceId }
                        ?.displayName
                        ?: "Sensor del historial"
                    "$displayName · UUID ${shortDeviceId(selectedDeviceId)}"
                },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "$validHits válidos · ${candidateHits.size - validHits} descartados · " +
                    "${state.selectedHitKeys.size} seleccionados",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSelectAllHits,
                    enabled = validHits > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("Todos los válidos") }
                OutlinedButton(
                    onClick = onClearHitSelection,
                    enabled = state.selectedHitKeys.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text("Limpiar") }
            }
        }

        if (candidateHits.isEmpty()) {
            item {
                EmptyCard(
                    if (activeInstallation == null) {
                        "No hay un montaje activo para consultar o guardar golpes."
                    } else {
                        "Activa Golpes. Cada impacto válido aparecerá aquí; " +
                            "puedes reunir tantos como necesites."
                    },
                )
            }
        } else {
            items(candidateHits, key = { it.stableKey() }) { hit ->
                HitCard(
                    hit = hit,
                    trace = tracesByKey[hit.stableKey()],
                    selected = hit.stableKey() in state.selectedHitKeys,
                    onToggle = { onToggleHit(hit) },
                    onDelete = { pendingDeleteHit = hit },
                )
            }
        }

        item {
            OutlinedTextField(
                value = stateName,
                onValueChange = { stateName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del nuevo estado") },
                placeholder = { Text("Ej. Estado inicial") },
                singleLine = true,
            )
        }
        item {
            Button(
                onClick = {
                    onRegisterState(stateName)
                    stateName = ""
                },
                enabled = state.selectedHitKeys.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Registrar estado con ${state.selectedHitKeys.size} golpes")
            }
        }
    }
}

@Composable
private fun ActiveStructureCard(
    structure: StructureEntity?,
    installation: SensorInstallationEntity?,
    canChange: Boolean,
    onChange: () -> Unit,
) {
    val mounted = installation != null && structure != null
    SectionCard(
        title = "Estructura y montaje",
        subtitle = if (mounted) structure?.name else "Sin montaje activo",
        accent = if (mounted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        trailing = {
            StatusPill(
                text = stringResource(
                    if (mounted) {
                        R.string.monitoring_mount_status_active
                    } else {
                        R.string.monitoring_mount_status_required
                    },
                ),
                container = if (mounted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                content = if (mounted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
        },
    ) {
        if (!mounted) {
            Text(
                "Las lecturas en vivo no se guardarán hasta elegir dónde está " +
                    "instalado el sensor.",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            val currentInstallation = requireNotNull(installation)
            val currentStructure = requireNotNull(structure)
            if (currentStructure.description.isNotBlank()) {
                Text(
                    currentStructure.description,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "Superficie: ${currentInstallation.surfaceType} · " +
                    "ubicación: " +
                    currentInstallation.locationDescription.ifBlank { "sin detalle" },
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Montaje iniciado ${dateTime(currentInstallation.installedAtEpochMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onChange,
            enabled = canChange,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (installation == null) "Elegir estructura" else "Cambiar montaje")
        }
        if (!canChange) {
            Text(
                "Conecta el ESP32 para crear un montaje y reiniciar su calibración.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StructureSetupDialog(
    structures: List<StructureEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String?, String, String, String, String) -> Unit,
) {
    var createNew by rememberSaveable { mutableStateOf(structures.isEmpty()) }
    var selectedStructureId by rememberSaveable {
        mutableStateOf(structures.firstOrNull()?.structureId)
    }
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var surface by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    val valid = if (createNew) name.isNotBlank() else selectedStructureId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo montaje") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Cambiar de montaje cierra el anterior, pone el sensor en reposo " +
                        "y reinicia su algoritmo.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (structures.isNotEmpty()) {
                    Text("Estructura existente", fontWeight = FontWeight.SemiBold)
                    structures.forEach { structure ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    createNew = false
                                    selectedStructureId = structure.structureId
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected =
                                    !createNew &&
                                        selectedStructureId == structure.structureId,
                                onClick = {
                                    createNew = false
                                    selectedStructureId = structure.structureId
                                },
                            )
                            Column {
                                Text(structure.name)
                                if (structure.description.isNotBlank()) {
                                    Text(
                                        structure.description,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { createNew = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = createNew,
                        onClick = { createNew = true },
                    )
                    Text("Crear una estructura nueva")
                }
                if (createNew) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej. Base de cartón B") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción opcional") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = surface,
                    onValueChange = { surface = it },
                    label = { Text("Tipo de superficie") },
                    placeholder = { Text("Cartón, concreto, madera…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación del sensor") },
                    placeholder = { Text("Centro, esquina superior…") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedStructureId.takeUnless { createNew },
                        name,
                        description,
                        surface,
                        location,
                    )
                },
                enabled = valid,
            ) { Text("Iniciar y calibrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun ConnectionCard(
    state: IotUiState,
    onSearch: () -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
) {
    val ready = state.linkState == LinkState.READY
    val active = state.linkState == LinkState.SCANNING ||
        state.linkState == LinkState.CONNECTING ||
        state.linkState == LinkState.DISCOVERING
    val accent = when {
        ready -> MaterialTheme.colorScheme.primary
        active -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    SectionCard(
        title = "Conexión BLE",
        subtitle = when (state.linkState) {
            LinkState.READY -> "Conectado a ${state.connectedAddress.orEmpty()}"
            LinkState.SCANNING -> "Buscando sensores cercanos…"
            else -> state.linkMessage
        },
        accent = accent,
        trailing = {
            StatusPill(
                text = when {
                    ready -> stringResource(R.string.monitoring_connection_status_online)
                    active -> stringResource(R.string.monitoring_connection_status_progress)
                    else -> stringResource(R.string.monitoring_connection_status_offline)
                },
                container = when {
                    ready -> MaterialTheme.colorScheme.primaryContainer
                    active -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                content = when {
                    ready -> MaterialTheme.colorScheme.onPrimaryContainer
                    active -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                pulse = ready || active,
            )
        },
    ) {
        state.identityConflict?.let { conflict ->
            Text(
                conflict,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.operationalStatus?.identityWarning == true) {
            Text(
                "Advertencia: identidad temporal del ESP32; su UUID puede " +
                    "cambiar al reiniciar porque no quedó guardado en NVS.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ready) {
                OutlinedButton(onClick = onRefresh) { Text("Actualizar") }
                OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
            } else {
                Button(onClick = onSearch) { Text("Buscar sensor") }
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: MeasurementMode,
    enabled: Boolean,
    onHitsEnabled: (Boolean) -> Unit,
    onSeismicEnabled: (Boolean) -> Unit,
) {
    SectionCard(
        title = "Modo del sensor",
        subtitle = stringResource(R.string.monitoring_mode_subtitle),
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        ModeSwitch(
            title = "Golpes controlados",
            detail = "Captura impactos individuales para crear estados.",
            checked = mode == MeasurementMode.HITS,
            enabled = enabled,
            onCheckedChange = onHitsEnabled,
        )
        HorizontalDivider()
        ModeSwitch(
            title = "Vigilancia sísmica",
            detail = "Detecta movimiento sostenido como un evento, no como golpes.",
            checked = mode == MeasurementMode.SEISMIC,
            enabled = enabled,
            onCheckedChange = onSeismicEnabled,
        )
        Text(
            if (mode == MeasurementMode.REST) {
                "Ambos apagados: reposo. No se detectan golpes ni sismos."
            } else {
                "Los modos son excluyentes; al activar uno se desactiva el otro."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun GuidanceCard(
    mode: MeasurementMode,
    phase: OperationalPhase,
    progress: Int,
) {
    val guidance = measurementGuidanceState(mode, phase)
    val (title, instruction) = when (guidance) {
        MeasurementGuidanceState.CALIBRATING ->
            "1 · Calibrando" to "No toques la estructura. Espera a que termine."

        MeasurementGuidanceState.RESTING ->
            "Reposo" to "El sensor sólo mantiene su orientación y línea base."

        MeasurementGuidanceState.READY_FOR_HIT ->
            "2 · Listo para un golpe" to
                "Da un solo golpe breve y espera. Después podrás dar otro; no hay límite."

        MeasurementGuidanceState.CAPTURING ->
            "3 · Capturando (${progress.coerceIn(0, 100)} %)" to
                "No vuelvas a golpear. El ESP32 todavía está registrando la oscilación."

        MeasurementGuidanceState.ANALYZING ->
            "4 · Analizando" to
                "No golpees. Espera mientras el ESP32 calcula y valida la frecuencia."

        MeasurementGuidanceState.HIT_VALID ->
            "4 · Golpe aceptado" to
                "La medición fue válida. No golpees hasta que la estructura vuelva al reposo."

        MeasurementGuidanceState.HIT_DISCARDED ->
            "4 · Golpe descartado" to
                "La medición no pasó la validación. Espera al reposo antes de intentarlo otra vez."

        MeasurementGuidanceState.WAITING_FOR_REST ->
            "5 · Esperando reposo" to
                "No golpees. Mantén la estructura quieta hasta que la fase vuelva a «listo»."

        MeasurementGuidanceState.SEISMIC_WATCHING ->
            "Vigilancia activa" to
                "Déjalo en reposo. Un movimiento sostenido iniciará un Evento Sismo y " +
                    "terminará automáticamente cuando vuelva la calma."

        MeasurementGuidanceState.SEISMIC_ACTIVE ->
            "Evento sísmico en curso" to
                "No manipules el sensor. El ESP32 cerrará el evento cuando detecte calma."

        MeasurementGuidanceState.SYNCHRONIZING ->
            "Sincronizando fase" to
                "No golpees ni manipules el sensor hasta que el ESP32 confirme la fase."
    }
    val accent = when (guidance) {
        MeasurementGuidanceState.HIT_DISCARDED ->
            MaterialTheme.colorScheme.error

        MeasurementGuidanceState.WAITING_FOR_REST ->
            MaterialTheme.colorScheme.tertiary

        MeasurementGuidanceState.RESTING,
        MeasurementGuidanceState.SEISMIC_WATCHING,
        MeasurementGuidanceState.SEISMIC_ACTIVE,
        -> MaterialTheme.colorScheme.secondary

        else -> MaterialTheme.colorScheme.primary
    }
    SectionCard(
        title = title,
        accent = accent,
    ) {
        Text(instruction)
        if (
            guidance == MeasurementGuidanceState.CALIBRATING ||
            guidance == MeasurementGuidanceState.CAPTURING
        ) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = accent.copy(alpha = 0.14f),
            )
        }
    }
}

@Composable
private fun LiveMetrics(
    snapshot: StructuralSnapshot,
    mode: MeasurementMode,
    liveTelemetry: Boolean,
) {
    // En reposo las frecuencias no significan nada: pertenecen al último golpe,
    // que puede ser de hace rato. Lo útil ahí es comprobar que el IMU responde.
    if (mode == MeasurementMode.REST) {
        SensorCheckCard(snapshot = snapshot, liveTelemetry = liveTelemetry)
        return
    }
    SectionCard(
        title = "Lectura actual",
        subtitle = stringResource(R.string.monitoring_live_subtitle),
        accent = MaterialTheme.colorScheme.primary,
    ) {
        // Valores que se refrescan en vivo: monoespaciados para que la línea
        // no se desplace en cada notificación.
        MetricRow("Frecuencia del último golpe", hz(snapshot.fftFrequencyHz))
        MetricRow("Mediana provisional del ESP32", hz(snapshot.medianFrequencyHz))
        MetricRow("Cambio de inclinación", angle(snapshot.tiltChangeDeg))
        MetricRow(
            "Aceleración dinámica pico",
            "${decimal(snapshot.peakDynamicAccelerationMg)} mg",
        )
    }
}

/**
 * Comprobación del IMU en reposo.
 *
 * Separa deliberadamente los dos chips: el modelo 3D y la barra de movimiento
 * salen del acelerómetro, la barra de giro sale del giroscopio. Si sólo una
 * reacciona, el usuario sabe cuál falla — con un único indicador combinado eso
 * sería invisible.
 */
@Composable
private fun SensorCheckCard(
    snapshot: StructuralSnapshot,
    liveTelemetry: Boolean,
) {
    SectionCard(
        title = stringResource(R.string.monitoring_sensor_check_title),
        subtitle = stringResource(R.string.monitoring_sensor_check_subtitle),
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        if (!liveTelemetry) {
            // Sin firmware que notifique en reposo las barras no se moverían y
            // el usuario culparía al sensor. Se dice antes de que lo intente.
            Text(
                stringResource(R.string.monitoring_sensor_check_waiting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        MetricRow(
            stringResource(R.string.monitoring_sensor_check_gyro),
            "${decimal(snapshot.peakAngularVelocityDps)} °/s",
        )
        LevelBar(
            value = snapshot.peakAngularVelocityDps.toFloat(),
            maxValue = GYRO_BAR_MAX_DPS,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        MetricRow(
            stringResource(R.string.monitoring_sensor_check_accel),
            "${decimal(snapshot.peakDynamicAccelerationMg)} mg",
        )
        LevelBar(
            value = snapshot.peakDynamicAccelerationMg.toFloat(),
            maxValue = ACCEL_BAR_MAX_MG,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        MetricRow(
            stringResource(R.string.monitoring_sensor_check_tilt),
            angle(snapshot.tiltChangeDeg),
        )
        Text(
            stringResource(R.string.monitoring_sensor_check_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val GYRO_BAR_MAX_DPS = 120f
private const val ACCEL_BAR_MAX_MG = 600f

@Composable
private fun HitCard(
    hit: HitMeasurementEntity,
    trace: MeasurementTraceEntity?,
    selected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var traceExpanded by rememberSaveable(hit.stableKey()) {
        mutableStateOf(false)
    }
    val historyShift =
        trace?.rejectionCode == "FREQUENCY_SHIFT" ||
            hit.reason.contains("cambio historico", ignoreCase = true)
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hit.valid, onClick = onToggle),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = selected,
                    enabled = hit.valid,
                    onCheckedChange = { onToggle() },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Golpe #${hit.sequence} · ${hz(hit.fftFrequencyHz)}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Sensor ${shortDeviceId(hit.deviceId)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${dateTime(hit.receivedAtEpochMs)} · SNR ${decimal(hit.snrDb)} dB · " +
                            "roll ${angle(hit.rollDeg)} · pitch ${angle(hit.pitchDeg)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!hit.valid) {
                        // El snapshot recorta el motivo a 24 caracteres y se
                        // come justo los números —«Sin senal sobre ruido: 1»—.
                        // La traza lo transporta entero, así que se prefiere.
                        val reason = trace?.reason?.takeIf { it.isNotBlank() }
                            ?: hit.reason.ifBlank { "medición inválida" }
                        Text(
                            "Descartado: $reason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (historyShift) {
                        Text(
                            "Válido con advertencia: cambió respecto al historial del ESP32.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trace != null) {
                            TextButton(onClick = { traceExpanded = !traceExpanded }) {
                                Text(
                                    if (traceExpanded) {
                                        "Ocultar captura"
                                    } else {
                                        "Ver captura de 100 Hz"
                                    },
                                )
                            }
                        }
                        TextButton(onClick = onDelete) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (trace == null) {
                        Text(
                            "Sin traza: actualiza el firmware o solicita nuevamente la captura.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (trace != null && traceExpanded) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(12.dp)) {
                    MeasurementTraceDetails(trace)
                }
            }
        }
    }
}

@Composable
private fun SensorsTab(
    devices: List<ImuDeviceEntity>,
    connectedDeviceId: String?,
    selectedDeviceId: String?,
    structures: List<StructureEntity>,
    installations: List<SensorInstallationEntity>,
    activeInstallationId: String?,
    onSelectSensor: (ImuDeviceEntity) -> Unit,
    onForgetSensor: (ImuDeviceEntity) -> Unit,
    onDeleteStructureHistory: (StructureEntity) -> Unit,
    onClearAllTestHistory: () -> Unit,
    wifi: NodeWifiStatus?,
    wifiEditable: Boolean,
    onSetWifi: (String, String) -> Unit,
) {
    var pendingForget by remember { mutableStateOf<ImuDeviceEntity?>(null) }
    var pendingStructureDelete by remember { mutableStateOf<StructureEntity?>(null) }
    var confirmClearAll by rememberSaveable { mutableStateOf(false) }
    var clearConfirmation by rememberSaveable { mutableStateOf("") }
    pendingForget?.let { device ->
        AlertDialog(
            onDismissRequest = { pendingForget = null },
            title = { Text("¿Olvidar ${device.displayName}?") },
            text = {
                Text(
                    "Se quitará del registro de sensores y se cerrará su instalación " +
                        "activa. Los golpes, estados y sismos históricos se conservarán.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingForget = null
                        onForgetSensor(device)
                    },
                ) { Text("Olvidar sensor") }
            },
            dismissButton = {
                TextButton(onClick = { pendingForget = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
    pendingStructureDelete?.let { structure ->
        val installationCount = installations.count {
            it.structureId == structure.structureId
        }
        AlertDialog(
            onDismissRequest = { pendingStructureDelete = null },
            title = { Text("¿Eliminar ${structure.name}?") },
            text = {
                Text(
                    "Se eliminarán definitivamente sus $installationCount montajes, " +
                        "golpes, gráficas, estados, sismos y análisis. El registro del " +
                        "ESP32 no se borrará.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingStructureDelete = null
                        onDeleteStructureHistory(structure)
                    },
                ) { Text("Eliminar historial") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStructureDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = {
                confirmClearAll = false
                clearConfirmation = ""
            },
            title = { Text("¿Borrar todos los datos de prueba?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se eliminarán todas las estructuras, montajes, golpes, trazas, " +
                            "estados y sismos. Los sensores recordados se conservarán.",
                    )
                    OutlinedTextField(
                        value = clearConfirmation,
                        onValueChange = { clearConfirmation = it },
                        label = { Text("Escribe BORRAR") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = clearConfirmation.trim() == "BORRAR",
                    onClick = {
                        confirmClearAll = false
                        clearConfirmation = ""
                        onClearAllTestHistory()
                    },
                ) { Text("Borrar definitivamente") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmClearAll = false
                        clearConfirmation = ""
                    },
                ) { Text("Cancelar") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NodeWifiCard(
                wifi = wifi,
                enabled = wifiEditable,
                onSend = onSetWifi,
            )
        }
        item {
            SectionTitle("Sensores registrados")
            Text(
                "El UUID persistente identifica cada ESP32. Si un UUID ya registrado " +
                    "aparece desde otra dirección BLE, la app bloquea la actualización " +
                    "y avisa del conflicto. Este registro es lógico; el bonding cifrado " +
                    "será una evolución.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (devices.isEmpty()) {
            item {
                EmptyCard(
                    "Conecta un nodo desde Medir. Al recibir su identidad quedará " +
                        "registrado automáticamente.",
                )
            }
        } else {
            items(devices, key = { it.deviceId }) { device ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    device.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    if (device.deviceId == connectedDeviceId) {
                                        "Conectado ahora"
                                    } else if (device.deviceId == selectedDeviceId) {
                                        "Seleccionado para consultar"
                                    } else {
                                        "Registrado"
                                    },
                                    color = if (
                                        device.deviceId == connectedDeviceId ||
                                        device.deviceId == selectedDeviceId
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (
                                    device.deviceId != selectedDeviceId &&
                                    connectedDeviceId == null
                                ) {
                                    TextButton(onClick = { onSelectSensor(device) }) {
                                        Text("Seleccionar")
                                    }
                                }
                                OutlinedButton(onClick = { pendingForget = device }) {
                                    Text("Olvidar")
                                }
                            }
                        }
                        HorizontalDivider()
                        Text(
                            "UUID",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            device.deviceId,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Dirección observada: ${device.bleAddress ?: "no disponible"}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Primera alta: ${dateTime(device.registeredAtEpochMs)} · " +
                                "última vez: ${dateTime(device.lastSeenAtEpochMs)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("Estructuras e historial")
            Text(
                "Cada estructura mantiene separados sus montajes, golpes, estados y " +
                    "sismos, aunque se reutilice el mismo ESP32.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (structures.isEmpty()) {
            item {
                EmptyCard("Todavía no hay estructuras. Créala desde la pestaña Medir.")
            }
        } else {
            items(structures, key = { "structure-${it.structureId}" }) { structure ->
                val structureInstallations = installations.filter {
                    it.structureId == structure.structureId
                }
                val isActive = structureInstallations.any {
                    it.installationId == activeInstallationId
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    structure.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    if (isActive) {
                                        "Montaje activo"
                                    } else {
                                        "${structureInstallations.size} montajes"
                                    },
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            TextButton(onClick = { pendingStructureDelete = structure }) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (structure.description.isNotBlank()) {
                            Text(
                                structure.description,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            "Creada ${dateTime(structure.createdAtEpochMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { confirmClearAll = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Borrar todos los datos de prueba")
            }
        }
    }
}

/**
 * Historial: estados registrados y sismos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(
    state: IotUiState,
    registeredStates: List<RegisteredStateEntity>,
    seismicEvents: List<SeismicEventEntity>,
    seismicAnalyses: List<SeismicAnalysisEntity>,
    structures: List<StructureEntity>,
    installations: List<SensorInstallationEntity>,
    onToggleComparison: (RegisteredStateEntity) -> Unit,
    onClearComparison: () -> Unit,
    onSelectSeismicEvent: (String) -> Unit,
    onSelectBeforeState: (String) -> Unit,
    onSelectAfterState: (String) -> Unit,
    onSaveSeismicAnalysis: () -> Unit,
) {
    var showSeismic by rememberSaveable { mutableStateOf(false) }
    val segments = listOf(
        stringResource(R.string.history_segment_states) to false,
        stringResource(R.string.history_segment_seismic) to true,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            segments.forEachIndexed { index, (label, seismic) ->
                SegmentedButton(
                    selected = showSeismic == seismic,
                    onClick = { showSeismic = seismic },
                    shape = SegmentedButtonDefaults.itemShape(index, segments.size),
                ) { Text(label) }
            }
        }
        if (showSeismic) {
            SeismicAnalysisTab(
                events = seismicEvents,
                states = registeredStates,
                analyses = seismicAnalyses,
                structures = structures,
                installations = installations,
                selectedEventId = state.selectedSeismicEventId,
                beforeStateId = state.beforeStateId,
                afterStateId = state.afterStateId,
                onSelectEvent = onSelectSeismicEvent,
                onSelectBefore = onSelectBeforeState,
                onSelectAfter = onSelectAfterState,
                onSave = onSaveSeismicAnalysis,
            )
        } else {
            StatesTab(
                states = registeredStates,
                structures = structures,
                installations = installations,
                selectedIds = state.selectedStateIds,
                onToggleComparison = onToggleComparison,
                onClearComparison = onClearComparison,
            )
        }
    }
}

@Composable
private fun StatesTab(
    states: List<RegisteredStateEntity>,
    structures: List<StructureEntity>,
    installations: List<SensorInstallationEntity>,
    selectedIds: List<String>,
    onToggleComparison: (RegisteredStateEntity) -> Unit,
    onClearComparison: () -> Unit,
) {
    val selected = selectedIds.mapNotNull { id -> states.firstOrNull { it.stateId == id } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Estados registrados")
            Text(
                "Cada estado combina sólo los golpes que escogiste. Selecciona dos " +
                    "para comparar su frecuencia y posición angular.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected.size == 2) {
            item { StateComparisonCard(selected[0], selected[1]) }
            item {
                OutlinedButton(
                    onClick = onClearComparison,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Limpiar comparación") }
            }
        }
        if (states.isEmpty()) {
            item { EmptyCard("Aún no hay estados. Créalo desde la pestaña Medición.") }
        } else {
            items(states, key = { it.stateId }) { registered ->
                RegisteredStateCard(
                    state = registered,
                    structureName = measurementContextName(
                        registered.installationId,
                        structures,
                        installations,
                    ),
                    selected = registered.stateId in selectedIds,
                    onClick = { onToggleComparison(registered) },
                )
            }
        }
    }
}

@Composable
private fun RegisteredStateCard(
    state: RegisteredStateEntity,
    structureName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(state.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "$structureName · sensor ${shortDeviceId(state.deviceId)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("${hz(state.medianFrequencyHz)} ± MAD ${hz(state.frequencyMadHz)}")
            Text(
                "${state.hitCount} golpes · ${dateTime(state.createdAtEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "roll ${angle(state.rollDeg)} · pitch ${angle(state.pitchDeg)} · " +
                    "inclinación ${angle(state.tiltFromReferenceDeg)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StateComparisonCard(
    before: RegisteredStateEntity,
    after: RegisteredStateEntity,
) {
    val frequencyDelta = after.medianFrequencyHz - before.medianFrequencyHz
    val percent = if (abs(before.medianFrequencyHz) > 0.0001) {
        frequencyDelta * 100.0 / before.medianFrequencyHz
    } else {
        0.0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Comparación", style = MaterialTheme.typography.titleMedium)
            Text("${before.name} → ${after.name}", fontWeight = FontWeight.SemiBold)
            Text("Δ frecuencia: ${signed(frequencyDelta)} Hz (${signed(percent)} %)")
            Text("Δ roll: ${signed(angleDelta(before.rollDeg, after.rollDeg))}°")
            Text("Δ pitch: ${signed(after.pitchDeg - before.pitchDeg)}°")
        }
    }
}

@Composable
private fun SeismicAnalysisTab(
    events: List<SeismicEventEntity>,
    states: List<RegisteredStateEntity>,
    analyses: List<SeismicAnalysisEntity>,
    structures: List<StructureEntity>,
    installations: List<SensorInstallationEntity>,
    selectedEventId: String?,
    beforeStateId: String?,
    afterStateId: String?,
    onSelectEvent: (String) -> Unit,
    onSelectBefore: (String) -> Unit,
    onSelectAfter: (String) -> Unit,
    onSave: () -> Unit,
) {
    val selectedEvent = events.firstOrNull { it.eventId == selectedEventId }
    val compatibleStates = selectedEvent?.let { event ->
        states.filter {
            sameInstallationContext(
                firstDeviceId = it.deviceId,
                firstInstallationId = it.installationId,
                secondDeviceId = event.deviceId,
                secondInstallationId = event.installationId,
            )
        }
    }.orEmpty()
    val eventEnd = selectedEvent?.let {
        it.endedAtEpochMs ?: it.startedAtEpochMs
    }
    val beforeCandidates = selectedEvent?.let { event ->
        compatibleStates
            .filter {
                isMeasurementStateBeforeEvent(
                    stateLastHitAtEpochMs = it.lastHitAtEpochMs,
                    eventStartedAtEpochMs = event.startedAtEpochMs,
                )
            }
            .sortedByDescending { it.lastHitAtEpochMs }
    }.orEmpty()
    val afterCandidates = if (eventEnd == null) {
        emptyList()
    } else {
        compatibleStates
            .filter {
                isMeasurementStateAfterEvent(
                    stateFirstHitAtEpochMs = it.firstHitAtEpochMs,
                    eventEndedAtEpochMs = eventEnd,
                )
            }
            .sortedBy { it.firstHitAtEpochMs }
    }
    val before = beforeCandidates.firstOrNull { it.stateId == beforeStateId }
    val after = afterCandidates.firstOrNull { it.stateId == afterStateId }
    val saved = selectedEventId
        ?.let { id -> analyses.firstOrNull { it.eventId == id } }
        ?.takeIf { analysis ->
            beforeCandidates.any { it.stateId == analysis.beforeStateId } &&
                afterCandidates.any { it.stateId == analysis.afterStateId }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Efecto de sismos")
            Text(
                "Selecciona el evento y los estados medidos inmediatamente antes y " +
                    "después. La app propone los más cercanos por fecha.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (events.isEmpty()) {
            item {
                EmptyCard(
                    "No hay eventos. Activa Vigilancia sísmica y mantén el celular " +
                        "conectado para recibir el siguiente.",
                )
            }
        } else {
            item { SectionTitle("1 · Evento sísmico") }
            items(events, key = { it.eventId }) { event ->
                SeismicEventCard(
                    event = event,
                    structureName = measurementContextName(
                        event.installationId,
                        structures,
                        installations,
                    ),
                    selected = event.eventId == selectedEventId,
                    analyzed = hasValidTemporalAnalysis(event, states, analyses),
                    onClick = { onSelectEvent(event.eventId) },
                )
            }
        }

        selectedEvent?.let { event ->
            item { SectionTitle("2 · Estado inmediatamente anterior") }
            if (beforeCandidates.isEmpty()) {
                item {
                    EmptyCard(
                        "No existe un estado cuyas mediciones hayan terminado antes " +
                            "de comenzar este sismo.",
                    )
                }
            } else {
                items(beforeCandidates, key = { "before-${it.stateId}" }) { structuralState ->
                    StateChoice(
                        state = structuralState,
                        structureName = measurementContextName(
                            structuralState.installationId,
                            structures,
                            installations,
                        ),
                        selected = structuralState.stateId == beforeStateId,
                        temporalLabel = "mediciones anteriores al sismo",
                        onClick = { onSelectBefore(structuralState.stateId) },
                    )
                }
            }

            item { SectionTitle("3 · Estado inmediatamente posterior") }
            if (afterCandidates.isEmpty()) {
                item {
                    EmptyCard(
                        "Todavía no hay un estado medido después de este sismo. " +
                            "Vuelve a Golpes, registra el estado posterior y selecciónalo aquí.",
                    )
                }
            } else {
                items(afterCandidates, key = { "after-${it.stateId}" }) { structuralState ->
                    StateChoice(
                        state = structuralState,
                        structureName = measurementContextName(
                            structuralState.installationId,
                            structures,
                            installations,
                        ),
                        selected = structuralState.stateId == afterStateId,
                        temporalLabel = "mediciones posteriores al sismo",
                        onClick = { onSelectAfter(structuralState.stateId) },
                    )
                }
            }

            if (before != null && after != null) {
                item { StateComparisonCard(before, after) }
                item {
                    SeismicAngleCard(
                        event = event,
                        before = before,
                        after = after,
                    )
                }
            }
            item {
                Button(
                    onClick = onSave,
                    enabled = before != null && after != null && before.stateId != after.stateId,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (saved == null) "Guardar análisis" else "Actualizar análisis")
                }
            }
        }
    }
}

private fun hasValidTemporalAnalysis(
    event: SeismicEventEntity,
    states: List<RegisteredStateEntity>,
    analyses: List<SeismicAnalysisEntity>,
): Boolean {
    val analysis = analyses.firstOrNull { it.eventId == event.eventId } ?: return false
    val before = states.firstOrNull { it.stateId == analysis.beforeStateId } ?: return false
    val after = states.firstOrNull { it.stateId == analysis.afterStateId } ?: return false
    val eventEnd = event.endedAtEpochMs ?: event.startedAtEpochMs
    return sameInstallationContext(
        firstDeviceId = before.deviceId,
        firstInstallationId = before.installationId,
        secondDeviceId = event.deviceId,
        secondInstallationId = event.installationId,
    ) &&
        sameInstallationContext(
            firstDeviceId = after.deviceId,
            firstInstallationId = after.installationId,
            secondDeviceId = event.deviceId,
            secondInstallationId = event.installationId,
        ) &&
        isMeasurementStateBeforeEvent(
            stateLastHitAtEpochMs = before.lastHitAtEpochMs,
            eventStartedAtEpochMs = event.startedAtEpochMs,
        ) &&
        isMeasurementStateAfterEvent(
            stateFirstHitAtEpochMs = after.firstHitAtEpochMs,
            eventEndedAtEpochMs = eventEnd,
        )
}

@Composable
private fun SeismicEventCard(
    event: SeismicEventEntity,
    structureName: String,
    selected: Boolean,
    analyzed: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sismo #${event.eventSequence} · ${dateTime(event.startedAtEpochMs)}",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$structureName · sensor ${shortDeviceId(event.deviceId)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    "${decimal(event.durationSeconds)} s · PGA " +
                        "${decimal(event.peakAccelerationMg)} mg · " +
                        "dominante ${hz(event.dominantFrequencyHz)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (analyzed) {
                    Text(
                        "Análisis guardado",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StateChoice(
    state: RegisteredStateEntity,
    structureName: String,
    selected: Boolean,
    temporalLabel: String,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column {
                Text(state.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "$structureName · sensor ${shortDeviceId(state.deviceId)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    "${hz(state.medianFrequencyHz)} · ${dateTime(state.createdAtEpochMs)} · " +
                        temporalLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SeismicAngleCard(
    event: SeismicEventEntity,
    before: RegisteredStateEntity,
    after: RegisteredStateEntity,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Cambio angular", style = MaterialTheme.typography.titleMedium)
            Text(
                "Estados: roll ${signed(angleDelta(before.rollDeg, after.rollDeg))}° · " +
                    "pitch ${signed(after.pitchDeg - before.pitchDeg)}°",
            )
            Text(
                "Durante el evento: roll " +
                    "${signed(angleDelta(event.rollBeforeDeg, event.rollAfterDeg))}° · " +
                    "pitch ${signed(event.pitchAfterDeg - event.pitchBeforeDeg)}°",
            )
            Text("Cambio de inclinación detectado: ${angle(event.tiltChangeDeg)}")
        }
    }
}

private fun measurementContextName(
    installationId: String?,
    structures: List<StructureEntity>,
    installations: List<SensorInstallationEntity>,
): String {
    if (installationId == null) return "Historial sin asignar"
    val structureId = installations
        .firstOrNull { it.installationId == installationId }
        ?.structureId
        ?: return "Montaje eliminado"
    return structures.firstOrNull { it.structureId == structureId }?.name
        ?: "Estructura eliminada"
}
