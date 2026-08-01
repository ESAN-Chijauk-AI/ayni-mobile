package com.ayni.mobile.ui.iot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.data.iot.DeviceEnrollmentSupersededException
import com.ayni.mobile.data.iot.DeviceIdentityConflictException
import com.ayni.mobile.data.iot.StructuralStateRepository
import com.ayni.mobile.data.iot.device.NodeClientFactory
import com.ayni.mobile.data.iot.device.SensorNodeClient
import com.ayni.mobile.data.iot.local.HitMeasurementEntity
import com.ayni.mobile.data.iot.local.ImuDeviceEntity
import com.ayni.mobile.data.iot.local.RegisteredStateEntity
import com.ayni.mobile.data.iot.local.SeismicEventEntity
import com.ayni.mobile.data.iot.local.SensorInstallationEntity
import com.ayni.mobile.data.iot.local.StructureEntity
import com.ayni.mobile.domain.iot.DiscoveredNode
import com.ayni.mobile.domain.iot.LinkState
import com.ayni.mobile.domain.iot.MeasurementMode
import com.ayni.mobile.domain.iot.MeasurementTrace
import com.ayni.mobile.domain.iot.OperationalStatus
import com.ayni.mobile.domain.iot.ProtocolEvent
import com.ayni.mobile.domain.iot.SeismicEventRecord
import com.ayni.mobile.domain.iot.StructuralSnapshot
import com.ayni.mobile.domain.iot.WifiCredentialsProblem
import com.ayni.mobile.domain.iot.isMeasurementStateAfterEvent
import com.ayni.mobile.domain.iot.isMeasurementStateBeforeEvent
import com.ayni.mobile.domain.iot.sameInstallationContext
import com.ayni.mobile.domain.iot.shortDeviceId
import com.ayni.mobile.domain.iot.validateWifiCredentials
import com.ayni.mobile.domain.iot.wifiCommandArgument
import com.ayni.mobile.domain.model.StructuralHitReading
import com.ayni.mobile.domain.usecase.AnalyzeStructuralHitsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sesión del nodo ESP32: enlace BLE, ingestión de telemetría, política de ACK y de
 * olvido. Migrado de `MainViewModel` de ProtoEstados; el comportamiento es idéntico,
 * sólo cambia la inyección (Hilt en vez de AppContainer manual) y el paquete.
 *
 * `NodeClientFactory` se invoca con `this` porque el cliente necesita su listener al
 * construirse; con FakeNodeClient permite trabajar sin ESP32.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val repository: StructuralStateRepository,
    private val analyzeStructuralHits: AnalyzeStructuralHitsUseCase,
    nodeClientFactory: NodeClientFactory,
) : ViewModel(),
    SensorNodeClient.Listener {

    private val gateway = nodeClientFactory.create(this)
    private val mutableUiState = MutableStateFlow(IotUiState())
    private val forgottenAtGeneration = mutableMapOf<String, Long>()
    private val suppressedInstallationIds = mutableSetOf<String>()
    private var connectionGeneration = 0L
    private var enrollmentStartedAtEpochMs = 0L
    private var acceptsTelemetry = false
    private var validatedDeviceId: String? = null
    private var manualActiveInstallation: SensorInstallationEntity? = null

    /**
     * Última clave `(deviceId, sessionId, sequence)` escrita en Room. Se limpia
     * al conectar para que la primera notificación de cada enlace se persista
     * siempre: puede ser un golpe que quedó sin guardar en la sesión anterior.
     */
    private var lastPersistedSnapshotKey: String? = null
    private var lastPersistedReadingAtEpochMs = 0L
    private var lastPersistedReadingContext: String? = null

    val uiState: StateFlow<IotUiState> = mutableUiState.asStateFlow()
    val activeInstallation = mutableUiState
        .map { it.selectedDeviceId }
        .distinctUntilChanged()
        .flatMapLatest { deviceId ->
            if (deviceId == null) flowOf(null)
            else repository.observeActiveInstallation(deviceId)
                .onStart { emit(null) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )
    val candidateHits = activeInstallation
        .flatMapLatest { installation ->
            if (installation == null) flowOf(emptyList())
            else repository.observeCandidateHits(
                installation.deviceId,
                installation.installationId,
            )
                .onStart { emit(emptyList()) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
    val measurementTraces = activeInstallation
        .flatMapLatest { installation ->
            if (installation == null) flowOf(emptyList())
            else repository.observeMeasurementTraces(
                installation.deviceId,
                installation.installationId,
            )
                .onStart { emit(emptyList()) }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
    val rememberedDevices = repository.rememberedDevices.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val structures = repository.structures.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val installations = repository.installations.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val registeredStates = repository.registeredStates.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val seismicEvents = repository.seismicEvents.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val seismicAnalyses = repository.seismicAnalyses.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun setPermissionsGranted(granted: Boolean) {
        mutableUiState.update {
            it.copy(
                permissionsGranted = granted,
                error = if (granted) null else "Se requieren permisos de Bluetooth cercano",
            )
        }
    }

    fun startScan() {
        if (!mutableUiState.value.permissionsGranted) {
            onError("Autoriza Bluetooth cercano antes de buscar")
            return
        }
        gateway.startScan()
    }

    fun connect(node: DiscoveredNode) {
        connectionGeneration++
        enrollmentStartedAtEpochMs = System.currentTimeMillis()
        acceptsTelemetry = true
        validatedDeviceId = null
        manualActiveInstallation = null
        lastPersistedSnapshotKey = null
        lastPersistedReadingAtEpochMs = 0L
        lastPersistedReadingContext = null
        mutableUiState.update {
            it.copy(
                connectedAddress = node.address,
                selectedDeviceId = null,
                operationalStatus = null,
                currentSnapshot = null,
                currentTrace = null,
                selectedHitKeys = emptySet(),
                identityConflict = null,
                linkMessage = "Conectando con ${node.name}…",
                error = null,
            )
        }
        gateway.connect(node.address)
    }

    fun disconnect() {
        acceptsTelemetry = false
        validatedDeviceId = null
        manualActiveInstallation = null
        gateway.disconnect()
    }

    fun refresh() {
        gateway.sendCommand("GET_STATUS")
        gateway.sendCommand("GET_STATE")
        gateway.sendCommand("GET_TRACE")
        gateway.sendCommand("GET_LAST_SEISMIC")
    }

    fun resetAlgorithm() {
        gateway.sendCommand("RESET_ALGORITHM")
    }

    fun startInstallation(
        existingStructureId: String?,
        structureName: String,
        structureDescription: String,
        surfaceType: String,
        locationDescription: String,
    ) {
        val state = mutableUiState.value
        val deviceId = validatedDeviceId
        if (state.linkState != LinkState.READY || deviceId == null) {
            onError("Conecta y valida el sensor antes de iniciar un montaje")
            return
        }
        gateway.sendCommand("SET_MODE", 0L)
        mutableUiState.update {
            it.copy(
                measurementMode = MeasurementMode.REST,
                selectedHitKeys = emptySet(),
            )
        }
        viewModelScope.launch {
            runCatching {
                repository.startInstallation(
                    deviceId = deviceId,
                    existingStructureId = existingStructureId,
                    structureName = structureName,
                    structureDescription = structureDescription,
                    surfaceType = surfaceType,
                    locationDescription = locationDescription,
                )
            }.onSuccess { installation ->
                manualActiveInstallation = installation
                suppressedInstallationIds.remove(installation.installationId)
                gateway.sendCommand("RESET_ALGORITHM")
                mutableUiState.update {
                    it.copy(
                        measurementMode = MeasurementMode.REST,
                        currentSnapshot = null,
                        currentTrace = null,
                        selectedHitKeys = emptySet(),
                        selectedStateIds = emptyList(),
                        message = "Nuevo montaje activo; el ESP32 volverá a calibrarse",
                    )
                }
            }.onFailure {
                onError(it.message ?: "No se pudo iniciar el montaje")
            }
        }
    }

    /**
     * Cambia la red del nodo. Se valida antes de enviar para poder explicar el
     * motivo: el firmware rechazaría igual, pero el rechazo llega como un evento
     * suelto y sin contexto.
     *
     * La clave viaja en claro por BLE. Es una decisión asumida para uso en
     * demostración; ver el README del firmware.
     */
    fun setNodeWifi(ssid: String, password: String) {
        if (mutableUiState.value.linkState != LinkState.READY) {
            onError("Conecta el sensor antes de cambiar su red")
            return
        }
        val trimmedSsid = ssid.trim()
        when (validateWifiCredentials(trimmedSsid, password)) {
            WifiCredentialsProblem.SSID_REQUIRED ->
                onError("Escribe el nombre de la red")

            WifiCredentialsProblem.SSID_TOO_LONG ->
                onError("El nombre de la red supera los 32 bytes")

            WifiCredentialsProblem.PASSWORD_TOO_SHORT ->
                onError("La clave debe tener al menos 8 caracteres, o ninguno si la red es abierta")

            WifiCredentialsProblem.PASSWORD_TOO_LONG ->
                onError("La clave supera los 63 bytes")

            WifiCredentialsProblem.DOES_NOT_FIT_IN_COMMAND ->
                onError("La red y la clave juntas no caben en un comando BLE")

            null -> {
                gateway.sendTextCommand(
                    "SET_WIFI",
                    wifiCommandArgument(trimmedSsid, password),
                )
                mutableUiState.update {
                    it.copy(message = "Red enviada al sensor; reconectando…")
                }
            }
        }
    }

    fun cancelAttempt() {
        gateway.sendCommand("CANCEL_ATTEMPT")
    }

    fun setHitsEnabled(enabled: Boolean) {
        setMeasurementMode(if (enabled) MeasurementMode.HITS else MeasurementMode.REST)
    }

    fun setSeismicEnabled(enabled: Boolean) {
        setMeasurementMode(if (enabled) MeasurementMode.SEISMIC else MeasurementMode.REST)
    }

    private fun setMeasurementMode(mode: MeasurementMode) {
        if (mutableUiState.value.linkState != LinkState.READY) {
            onError("Conecta el sensor antes de cambiar el modo")
            return
        }
        val deviceId = validatedDeviceId
        if (
            mode != MeasurementMode.REST &&
            (deviceId == null || currentInstallationFor(deviceId) == null)
        ) {
            onError("Selecciona una estructura y crea un montaje antes de medir")
            return
        }
        val argument = when (mode) {
            MeasurementMode.REST -> 0L
            MeasurementMode.HITS -> 1L
            MeasurementMode.SEISMIC -> 2L
        }
        if (gateway.sendCommand("SET_MODE", argument) != null) {
            mutableUiState.update {
                it.copy(
                    measurementMode = mode,
                    message = when (mode) {
                        MeasurementMode.REST -> "Sensor en reposo"
                        MeasurementMode.HITS -> "Esperando golpes controlados"
                        MeasurementMode.SEISMIC -> "Vigilancia sísmica activa"
                    },
                )
            }
        }
    }

    fun toggleCandidateHit(hit: HitMeasurementEntity) {
        if (!hit.valid) return
        if (hit.deviceId != mutableUiState.value.selectedDeviceId) {
            onError("Ese golpe pertenece a otro sensor")
            return
        }
        val activeInstallationId =
            currentInstallationFor(hit.deviceId)?.installationId
        if (activeInstallationId == null || hit.installationId != activeInstallationId) {
            onError("Ese golpe pertenece a otro montaje")
            return
        }
        val key = hit.stableKey()
        mutableUiState.update { current ->
            val selection = current.selectedHitKeys.toMutableSet()
            if (!selection.add(key)) selection.remove(key)
            current.copy(selectedHitKeys = selection)
        }
    }

    fun selectAllValidHits() {
        val selectedDeviceId = mutableUiState.value.selectedDeviceId ?: run {
            onError("Selecciona un sensor antes de escoger golpes")
            return
        }
        val activeInstallationId =
            currentInstallationFor(selectedDeviceId)?.installationId ?: run {
            onError("Selecciona un montaje antes de escoger golpes")
            return
        }
        mutableUiState.update {
            it.copy(
                selectedHitKeys = candidateHits.value
                    .filter { hit ->
                        hit.valid &&
                            hit.deviceId == selectedDeviceId &&
                            hit.installationId == activeInstallationId
                    }
                    .mapTo(linkedSetOf()) { hit -> hit.stableKey() },
            )
        }
    }

    fun clearHitSelection() {
        mutableUiState.update { it.copy(selectedHitKeys = emptySet()) }
    }

    fun analyzeLatestValidHits() {
        val deviceId = mutableUiState.value.selectedDeviceId
        val installation = currentInstallationFor(deviceId)
        if (deviceId == null || installation == null) {
            mutableUiState.update {
                it.copy(
                    structuralSafety = StructuralSafetyUiState.Failure(
                        "Selecciona un sensor y un montaje antes de analizar.",
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(structuralSafety = StructuralSafetyUiState.Working)
            }
            val hits = runCatching {
                repository.findLatestValidHits(
                    deviceId = deviceId,
                    installationId = installation.installationId,
                    limit = AnalyzeStructuralHitsUseCase.MAXIMUM_HIT_COUNT,
                )
            }.getOrElse { error ->
                mutableUiState.update {
                    it.copy(
                        structuralSafety = StructuralSafetyUiState.Failure(
                            error.message ?: "No se pudieron leer los golpes guardados.",
                        ),
                    )
                }
                return@launch
            }

            if (hits.size < AnalyzeStructuralHitsUseCase.MINIMUM_HIT_COUNT) {
                mutableUiState.update {
                    it.copy(
                        structuralSafety = StructuralSafetyUiState.InsufficientData(
                            validHitCount = hits.size,
                            requiredCount = AnalyzeStructuralHitsUseCase.MINIMUM_HIT_COUNT,
                        ),
                    )
                }
                return@launch
            }

            val readings = hits
                .sortedBy(HitMeasurementEntity::receivedAtEpochMs)
                .map { hit ->
                    StructuralHitReading(
                        measuredAtEpochMs = hit.receivedAtEpochMs,
                        frequencyHz = hit.fftFrequencyHz,
                        autocorrelationFrequencyHz = hit.autocorrelationFrequencyHz,
                        snrDb = hit.snrDb,
                        periodicity = hit.periodicity,
                        tiltChangeDeg = hit.tiltChangeDeg,
                        peakAccelerationMg = hit.peakDynamicAccelerationMg,
                        peakAngularVelocityDps = hit.peakAngularVelocityDps,
                        reason = hit.reason,
                    )
                }
            runCatching { analyzeStructuralHits(readings) }
                .onSuccess { analysis ->
                    mutableUiState.update {
                        it.copy(
                            structuralSafety = StructuralSafetyUiState.Success(
                                analysis = analysis,
                                validHitCount = hits.size,
                                maximumHitCount = AnalyzeStructuralHitsUseCase.MAXIMUM_HIT_COUNT,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    mutableUiState.update {
                        it.copy(
                            structuralSafety = StructuralSafetyUiState.Failure(
                                error.message ?: "Gemma no pudo completar el análisis.",
                            ),
                        )
                    }
                }
        }
    }

    fun deleteHit(hit: HitMeasurementEntity) {
        viewModelScope.launch {
            runCatching { repository.deleteHit(hit) }
                .onSuccess {
                    val key = hit.stableKey()
                    val affectsLiveAlgorithm =
                        hit.valid &&
                            validatedDeviceId == hit.deviceId &&
                            currentInstallationFor(hit.deviceId)?.installationId ==
                            hit.installationId &&
                            mutableUiState.value.linkState == LinkState.READY
                    if (affectsLiveAlgorithm) {
                        gateway.sendCommand("RESET_ALGORITHM")
                    }
                    mutableUiState.update { current ->
                        current.copy(
                            selectedHitKeys = current.selectedHitKeys - key,
                            currentSnapshot =
                                current.currentSnapshot?.takeUnless {
                                    it.deviceId == hit.deviceId &&
                                        it.sessionId == hit.sessionId &&
                                        it.sequence == hit.sequence
                                },
                            currentTrace =
                                current.currentTrace?.takeUnless {
                                    it.deviceId == hit.deviceId &&
                                        it.sessionId == hit.sessionId &&
                                        it.sequence == hit.sequence
                                },
                            message = if (affectsLiveAlgorithm) {
                                "Golpe eliminado; el ESP32 volverá a calibrarse"
                            } else {
                                "Golpe y captura eliminados"
                            },
                        )
                    }
                }
                .onFailure { onError(it.message ?: "No se pudo eliminar el golpe") }
        }
    }

    fun deleteStructureHistory(structure: StructureEntity) {
        val active = currentInstallationFor(validatedDeviceId)
        val installationIds = installations.value
            .filter { it.structureId == structure.structureId }
            .mapTo(mutableSetOf()) { it.installationId }
        active
            ?.takeIf { it.structureId == structure.structureId }
            ?.let { installationIds += it.installationId }
        suppressedInstallationIds += installationIds
        val affectsConnected =
            active?.structureId == structure.structureId &&
                mutableUiState.value.linkState == LinkState.READY
        if (affectsConnected) {
            gateway.sendCommand("SET_MODE", 0L)
            gateway.sendCommand("RESET_ALGORITHM")
        }
        viewModelScope.launch {
            runCatching { repository.deleteStructureHistory(structure.structureId) }
                .onSuccess {
                    if (manualActiveInstallation?.structureId == structure.structureId) {
                        manualActiveInstallation = null
                    }
                    mutableUiState.update {
                        it.copy(
                            measurementMode =
                                if (affectsConnected) MeasurementMode.REST
                                else it.measurementMode,
                            currentSnapshot =
                                if (affectsConnected) null else it.currentSnapshot,
                            currentTrace =
                                if (affectsConnected) null else it.currentTrace,
                            selectedHitKeys = emptySet(),
                            selectedStateIds = emptyList(),
                            selectedSeismicEventId = null,
                            beforeStateId = null,
                            afterStateId = null,
                            message = "Historial de “${structure.name}” eliminado",
                        )
                    }
                }
                .onFailure {
                    suppressedInstallationIds -= installationIds
                    onError(it.message ?: "No se pudo eliminar la estructura")
                }
        }
    }

    fun clearAllTestHistory() {
        val installationIds = installations.value
            .mapTo(mutableSetOf()) { it.installationId }
        manualActiveInstallation?.let { installationIds += it.installationId }
        activeInstallation.value?.let { installationIds += it.installationId }
        suppressedInstallationIds += installationIds
        val connected = mutableUiState.value.linkState == LinkState.READY
        if (connected) {
            gateway.sendCommand("SET_MODE", 0L)
            gateway.sendCommand("RESET_ALGORITHM")
        }
        viewModelScope.launch {
            runCatching { repository.clearAllTestHistory() }
                .onSuccess {
                    manualActiveInstallation = null
                    mutableUiState.update {
                        it.copy(
                            measurementMode = MeasurementMode.REST,
                            currentSnapshot = null,
                            currentTrace = null,
                            selectedHitKeys = emptySet(),
                            selectedStateIds = emptyList(),
                            selectedSeismicEventId = null,
                            beforeStateId = null,
                            afterStateId = null,
                            message = "Todos los datos de prueba fueron eliminados",
                        )
                    }
                }
                .onFailure {
                    suppressedInstallationIds -= installationIds
                    onError(it.message ?: "No se pudo borrar el historial")
                }
        }
    }

    fun registerSelectedState(name: String) {
        val selectedDeviceId = mutableUiState.value.selectedDeviceId ?: run {
            onError("Selecciona un sensor antes de registrar un estado")
            return
        }
        val activeInstallationId =
            currentInstallationFor(selectedDeviceId)?.installationId ?: run {
            onError("Selecciona una estructura y un montaje antes de registrar un estado")
            return
        }
        val selected = candidateHits.value.filter {
            it.deviceId == selectedDeviceId &&
                it.installationId == activeInstallationId &&
                it.stableKey() in mutableUiState.value.selectedHitKeys
        }
        if (selected.isEmpty()) {
            onError("Selecciona al menos un golpe válido")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.registerState(
                    name = name,
                    selectedHits = selected,
                    expectedDeviceId = selectedDeviceId,
                    expectedInstallationId = activeInstallationId,
                )
            }
                .onSuccess { state ->
                    mutableUiState.update {
                        it.copy(
                            selectedHitKeys = emptySet(),
                            message = "Estado “${state.name}” registrado con ${state.hitCount} golpes",
                        )
                    }
                }
                .onFailure { onError(it.message ?: "No se pudo registrar el estado") }
        }
    }

    fun selectSensor(device: ImuDeviceEntity) {
        val connectedDeviceId = mutableUiState.value.operationalStatus?.deviceId
        if (
            mutableUiState.value.linkState == LinkState.READY &&
            connectedDeviceId != null &&
            connectedDeviceId != device.deviceId
        ) {
            onError("Desconecta el sensor activo antes de consultar otro")
            return
        }
        manualActiveInstallation = null
        mutableUiState.update { current ->
            if (current.selectedDeviceId == device.deviceId) current
            else current.copy(
                selectedDeviceId = device.deviceId,
                selectedHitKeys = emptySet(),
                selectedStateIds = emptyList(),
                message = "Sensor seleccionado: ${device.displayName}",
            )
        }
    }

    fun toggleStateComparison(state: RegisteredStateEntity) {
        mutableUiState.update { current ->
            val selection = current.selectedStateIds.toMutableList()
            if (state.stateId in selection) {
                selection.remove(state.stateId)
                return@update current.copy(selectedStateIds = selection)
            }
            val selectedStates = current.selectedStateIds
                .mapNotNull { id ->
                    registeredStates.value.firstOrNull { it.stateId == id }
                }
            if (
                selectedStates.isNotEmpty() &&
                selectedStates.any { !sameMeasurementContext(it, state) }
            ) {
                return@update current.copy(
                    selectedStateIds = listOf(state.stateId),
                    message =
                        "La comparación se reinició: los estados deben ser del mismo montaje",
                )
            }
            if (selection.size == 2) selection.removeAt(0)
            selection.add(state.stateId)
            current.copy(selectedStateIds = selection)
        }
    }

    fun clearStateComparison() {
        mutableUiState.update { it.copy(selectedStateIds = emptyList()) }
    }

    fun forgetSensor(device: ImuDeviceEntity) {
        val state = mutableUiState.value
        val isCurrentIdentity =
            state.operationalStatus?.deviceId == device.deviceId ||
                state.currentSnapshot?.deviceId == device.deviceId ||
                validatedDeviceId == device.deviceId
        forgottenAtGeneration[device.deviceId] = connectionGeneration
        if (isCurrentIdentity) {
            currentInstallationFor(device.deviceId)?.let {
                suppressedInstallationIds += it.installationId
            }
            acceptsTelemetry = false
            validatedDeviceId = null
            manualActiveInstallation = null
            gateway.disconnect()
        }
        mutableUiState.update {
            it.copy(
                selectedDeviceId =
                    it.selectedDeviceId?.takeUnless { id -> id == device.deviceId },
                selectedHitKeys =
                    if (it.selectedDeviceId == device.deviceId) emptySet()
                    else it.selectedHitKeys,
                selectedStateIds =
                    it.selectedStateIds.filterNot { stateId ->
                        registeredStates.value.firstOrNull {
                            registered -> registered.stateId == stateId
                        }?.deviceId == device.deviceId
                    },
                operationalStatus =
                    it.operationalStatus?.takeUnless { status ->
                        status.deviceId == device.deviceId
                    },
                currentSnapshot =
                    it.currentSnapshot?.takeUnless { snapshot ->
                        snapshot.deviceId == device.deviceId
                    },
                currentTrace =
                    it.currentTrace?.takeUnless { trace ->
                        trace.deviceId == device.deviceId
                    },
            )
        }
        viewModelScope.launch {
            runCatching { repository.forgetDevice(device.deviceId) }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            message = "Sensor olvidado; sus mediciones se conservaron",
                        )
                    }
                }
                .onFailure { onError(it.message ?: "No se pudo olvidar el sensor") }
        }
    }

    fun selectSeismicEvent(eventId: String) {
        val event = seismicEvents.value.firstOrNull { it.eventId == eventId } ?: return
        val compatible = registeredStates.value.filter {
            sameMeasurementContext(it, event)
        }
        val eventEnd = event.endedAtEpochMs ?: event.startedAtEpochMs
        val beforeCandidates = compatible.filter {
            isMeasurementStateBeforeEvent(
                stateLastHitAtEpochMs = it.lastHitAtEpochMs,
                eventStartedAtEpochMs = event.startedAtEpochMs,
            )
        }
        val afterCandidates = compatible.filter {
            isMeasurementStateAfterEvent(
                stateFirstHitAtEpochMs = it.firstHitAtEpochMs,
                eventEndedAtEpochMs = eventEnd,
            )
        }
        val before = beforeCandidates.maxByOrNull { it.lastHitAtEpochMs }
        val after = afterCandidates.minByOrNull { it.firstHitAtEpochMs }
        val saved = seismicAnalyses.value.firstOrNull { it.eventId == eventId }
        val savedBefore = saved?.let { analysis ->
            beforeCandidates.firstOrNull { it.stateId == analysis.beforeStateId }
        }
        val savedAfter = saved?.let { analysis ->
            afterCandidates.firstOrNull { it.stateId == analysis.afterStateId }
        }
        val savedIsTemporallyValid = savedBefore != null && savedAfter != null
        mutableUiState.update {
            it.copy(
                selectedSeismicEventId = eventId,
                beforeStateId =
                    if (savedIsTemporallyValid) savedBefore?.stateId else before?.stateId,
                afterStateId =
                    if (savedIsTemporallyValid) savedAfter?.stateId else after?.stateId,
            )
        }
    }

    fun selectBeforeState(stateId: String) {
        mutableUiState.update { it.copy(beforeStateId = stateId) }
    }

    fun selectAfterState(stateId: String) {
        mutableUiState.update { it.copy(afterStateId = stateId) }
    }

    fun saveSeismicAnalysis() {
        val state = mutableUiState.value
        val eventId = state.selectedSeismicEventId
        val before = state.beforeStateId
        val after = state.afterStateId
        if (eventId == null || before == null || after == null) {
            onError("Selecciona el sismo y los estados anterior y posterior")
            return
        }
        val event = seismicEvents.value.firstOrNull { it.eventId == eventId }
        val beforeState = registeredStates.value.firstOrNull { it.stateId == before }
        val afterState = registeredStates.value.firstOrNull { it.stateId == after }
        if (
            event == null ||
            beforeState == null ||
            afterState == null ||
            !sameMeasurementContext(beforeState, event) ||
            !sameMeasurementContext(afterState, event)
        ) {
            onError("El sismo y ambos estados deben pertenecer al mismo montaje")
            return
        }
        val eventEnd = event.endedAtEpochMs ?: event.startedAtEpochMs
        if (
            !isMeasurementStateBeforeEvent(
                stateLastHitAtEpochMs = beforeState.lastHitAtEpochMs,
                eventStartedAtEpochMs = event.startedAtEpochMs,
            )
        ) {
            onError("El estado anterior contiene mediciones posteriores al inicio del sismo")
            return
        }
        if (
            !isMeasurementStateAfterEvent(
                stateFirstHitAtEpochMs = afterState.firstHitAtEpochMs,
                eventEndedAtEpochMs = eventEnd,
            )
        ) {
            onError("El estado posterior debe medirse después de terminar el sismo")
            return
        }
        viewModelScope.launch {
            runCatching { repository.saveSeismicAnalysis(eventId, before, after) }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(message = "Análisis sísmico guardado")
                    }
                }
                .onFailure { onError(it.message ?: "No se pudo guardar el análisis") }
        }
    }

    fun dismissError() {
        mutableUiState.update { it.copy(error = null) }
    }

    fun dismissMessage() {
        mutableUiState.update { it.copy(message = null) }
    }

    override fun onLinkState(state: LinkState, message: String) {
        if (state == LinkState.READY && !acceptsTelemetry) return
        if (state == LinkState.DISCONNECTED || state == LinkState.ERROR) {
            acceptsTelemetry = false
            validatedDeviceId = null
            manualActiveInstallation = null
            lastPersistedSnapshotKey = null
            lastPersistedReadingAtEpochMs = 0L
            lastPersistedReadingContext = null
        }
        mutableUiState.update {
            it.copy(
                linkState = state,
                linkMessage = message,
                connectedAddress = if (state == LinkState.DISCONNECTED) null
                else it.connectedAddress,
                operationalStatus = if (state == LinkState.DISCONNECTED) null
                else it.operationalStatus,
                error = if (state == LinkState.ERROR) message else it.error,
            )
        }
        if (state == LinkState.READY && acceptsTelemetry) refresh()
    }

    override fun onNodesChanged(nodes: List<DiscoveredNode>) {
        mutableUiState.update { it.copy(nodes = nodes) }
    }

    override fun onOperationalStatus(status: OperationalStatus) {
        val generation = connectionGeneration
        val observedAddress = mutableUiState.value.connectedAddress
        if (!canAcceptTelemetry(status.deviceId, generation)) return
        viewModelScope.launch {
            runCatching {
                repository.explicitlyRegisterDevice(
                    deviceId = status.deviceId,
                    displayName = status.displayName,
                    bleAddress = observedAddress,
                    enrollmentStartedAtEpochMs = enrollmentStartedAtEpochMs,
                )
                repository.findActiveInstallation(status.deviceId)
            }.onSuccess { installation ->
                if (!canAcceptTelemetry(status.deviceId, generation)) return@onSuccess
                val firstValidation = validatedDeviceId != status.deviceId
                validatedDeviceId = status.deviceId
                manualActiveInstallation = installation
                mutableUiState.update { current ->
                    val changedDevice = current.selectedDeviceId != status.deviceId
                    current.copy(
                        selectedDeviceId = status.deviceId,
                        selectedHitKeys =
                            if (changedDevice) emptySet() else current.selectedHitKeys,
                        selectedStateIds =
                            if (changedDevice) emptyList() else current.selectedStateIds,
                        operationalStatus = status,
                        measurementMode =
                            if (installation == null) MeasurementMode.REST
                            else status.measurementMode,
                        linkMessage = "Datos en vivo · ${status.displayName} · " +
                            shortDeviceId(status.deviceId),
                        identityConflict = null,
                    )
                }
                if (firstValidation) {
                    if (
                        installation == null &&
                        status.measurementMode != MeasurementMode.REST
                    ) {
                        gateway.sendCommand("SET_MODE", 0L)
                    }
                    // La primera sincronización puede llegar antes de validar el
                    // UUID. Se recupera una sola vez para no retransmitir la
                    // traza grande en cada cambio de fase.
                    gateway.sendCommand("GET_STATE")
                    gateway.sendCommand("GET_TRACE")
                    gateway.sendCommand("GET_LAST_SEISMIC")
                }
            }.onFailure { error ->
                if (!isCurrentGeneration(generation)) return@onFailure
                when (error) {
                    is DeviceIdentityConflictException ->
                        handleIdentityConflict(error, generation)

                    is DeviceEnrollmentSupersededException -> Unit
                    else ->
                        onError("No se registró la identidad del sensor: ${error.message}")
                }
            }
        }
    }

    override fun onStructuralSnapshot(snapshot: StructuralSnapshot) {
        val generation = connectionGeneration
        if (
            validatedDeviceId != snapshot.deviceId ||
            !canAcceptTelemetry(snapshot.deviceId, generation)
        ) {
            return
        }
        // La interfaz siempre ve el dato más reciente: es lo que mueve el modelo
        // 3D y las barras de comprobación del sensor.
        mutableUiState.update { it.copy(currentSnapshot = snapshot) }
        // Sin montaje no se persiste ni se confirma. No se emite un mensaje por
        // snapshot: la condición ya se anuncia de forma permanente en la tarjeta
        // "Estructura y montaje", y un aviso por notificación sería repetitivo.
        val installation = currentInstallationFor(snapshot.deviceId) ?: return
        val readingContext = "${snapshot.deviceId}|${installation.installationId}"
        if (lastPersistedReadingContext != readingContext) {
            lastPersistedReadingContext = readingContext
            lastPersistedReadingAtEpochMs = 0L
        }
        val receivedAt = System.currentTimeMillis()
        if (receivedAt - lastPersistedReadingAtEpochMs >= READING_SAMPLE_INTERVAL_MILLIS) {
            lastPersistedReadingAtEpochMs = receivedAt
            viewModelScope.launch {
                runCatching {
                    repository.persistStructuralReading(
                        snapshot = snapshot,
                        installationId = installation.installationId,
                        receivedAtEpochMs = receivedAt,
                    )
                }.onFailure {
                    lastPersistedReadingAtEpochMs = 0L
                    onError("No se guardó la lectura temporal: ${it.message}")
                }
            }
        }
        // El nodo repite el mismo `seq` con la orientación actualizada —en cada
        // vuelta a "listo", en cada GET_STATE y, con telemetría en vivo, varias
        // veces por segundo—. Persistirlo otra vez reescribiría el roll y el
        // pitch que quedaron registrados en ese golpe, y de ahí sale la
        // orientación de los estados registrados. Sólo se guarda cuando el nodo
        // publica una captura nueva.
        val key = "${snapshot.deviceId}|${snapshot.sessionId}|${snapshot.sequence}"
        if (key == lastPersistedSnapshotKey) return
        lastPersistedSnapshotKey = key
        viewModelScope.launch {
            runCatching {
                repository.persistSnapshot(
                    snapshot = snapshot,
                    activeInstallationId = installation.installationId,
                )
            }
                .onSuccess {
                    if (canAcceptTelemetry(snapshot.deviceId, generation)) {
                        gateway.sendCommand("ACK_STATE", snapshot.sessionId)
                    }
                }
                .onFailure {
                    // Se olvida la clave para que la siguiente notificación del
                    // mismo golpe pueda reintentar la escritura.
                    lastPersistedSnapshotKey = null
                    onError("No se guardó la medición: ${it.message}")
                }
        }
    }

    override fun onMeasurementTrace(trace: MeasurementTrace) {
        val generation = connectionGeneration
        if (
            validatedDeviceId != trace.deviceId ||
            !canAcceptTelemetry(trace.deviceId, generation)
        ) {
            return
        }
        mutableUiState.update { it.copy(currentTrace = trace) }
        val installation = currentInstallationFor(trace.deviceId) ?: return
        viewModelScope.launch {
            runCatching {
                repository.persistMeasurementTrace(
                    trace = trace,
                    activeInstallationId = installation.installationId,
                )
            }
                .onFailure {
                    onError("No se guardó la captura de 100 Hz: ${it.message}")
                }
        }
    }

    override fun onSeismicEvent(event: SeismicEventRecord) {
        val generation = connectionGeneration
        if (
            validatedDeviceId != event.deviceId ||
            !canAcceptTelemetry(event.deviceId, generation)
        ) {
            return
        }
        val installation = currentInstallationFor(event.deviceId)
        if (installation == null) {
            onError("El sismo no se guardó porque no hay un montaje activo")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.persistSeismicEvent(
                    event = event,
                    activeInstallationId = installation.installationId,
                )
            }
                .onSuccess {
                    if (!canAcceptTelemetry(event.deviceId, generation)) return@onSuccess
                    mutableUiState.update {
                        it.copy(message = "Evento sísmico registrado")
                    }
                }
                .onFailure { onError("No se guardó el sismo: ${it.message}") }
        }
    }

    override fun onProtocolEvent(event: ProtocolEvent) {
        if (!acceptsTelemetry) return
        mutableUiState.update {
            it.copy(linkMessage = event.detail.ifBlank { event.name })
        }
        if (event.name == "mode_changed") {
            gateway.sendCommand("GET_STATUS")
        }
    }

    override fun onProtocolDescription(json: String) {
        if (!acceptsTelemetry) return
        mutableUiState.update { it.copy(protocolDescription = json) }
    }

    override fun onError(message: String) {
        mutableUiState.update { it.copy(error = message) }
    }

    override fun onCleared() {
        gateway.close()
        super.onCleared()
    }

    private fun isCurrentGeneration(generation: Long): Boolean =
        acceptsTelemetry && generation == connectionGeneration

    private fun canAcceptTelemetry(deviceId: String, generation: Long): Boolean =
        isCurrentGeneration(generation) &&
            generation > (forgottenAtGeneration[deviceId] ?: Long.MIN_VALUE)

    private fun handleIdentityConflict(
        conflict: DeviceIdentityConflictException,
        generation: Long,
    ) {
        if (!isCurrentGeneration(generation)) return
        acceptsTelemetry = false
        validatedDeviceId = null
        forgottenAtGeneration[conflict.deviceId] = generation
        val message =
            "Conflicto de identidad: UUID ${shortDeviceId(conflict.deviceId)} ya estaba " +
                "asociado a ${conflict.knownAddress}, pero apareció en " +
                "${conflict.observedAddress}. No se actualizó la dirección."
        mutableUiState.update {
            it.copy(
                operationalStatus = null,
                currentSnapshot = null,
                currentTrace = null,
                selectedDeviceId = null,
                selectedHitKeys = emptySet(),
                identityConflict = message,
                error = message,
            )
        }
        gateway.disconnect()
    }

    private fun currentInstallationFor(deviceId: String?): SensorInstallationEntity? {
        if (deviceId == null) return null
        val installation = manualActiveInstallation
            ?.takeIf { it.deviceId == deviceId }
            ?: activeInstallation.value?.takeIf { it.deviceId == deviceId }
        return installation?.takeUnless {
            it.installationId in suppressedInstallationIds
        }
    }

    private fun sameMeasurementContext(
        first: RegisteredStateEntity,
        second: RegisteredStateEntity,
    ): Boolean =
        sameInstallationContext(
            firstDeviceId = first.deviceId,
            firstInstallationId = first.installationId,
            secondDeviceId = second.deviceId,
            secondInstallationId = second.installationId,
        )

    private fun sameMeasurementContext(
        state: RegisteredStateEntity,
        event: SeismicEventEntity,
    ): Boolean =
        sameInstallationContext(
            firstDeviceId = state.deviceId,
            firstInstallationId = state.installationId,
            secondDeviceId = event.deviceId,
            secondInstallationId = event.installationId,
        )

    companion object {
        private const val READING_SAMPLE_INTERVAL_MILLIS = 10_000L
    }
}
