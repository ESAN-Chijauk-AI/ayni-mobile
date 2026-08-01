package com.ayni.mobile.ui.proximity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.PeerConnectionState
import com.ayni.mobile.domain.proximity.ProximityRole
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.domain.proximity.SosReceptionState
import com.ayni.mobile.domain.usecase.ManageEmergencyProximityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProximityUiState(
    val role: ProximityRole? = null,
    val sosStatus: SosModeStatus = SosModeStatus.INACTIVE,
    val scanStatus: ProximityScanStatus = ProximityScanStatus.IDLE,
    val signals: List<NearbySosSignal> = emptyList(),
    val selectedPeerId: String? = null,
    val selectedSignal: NearbySosSignal? = null,
    val signalLost: Boolean = false,
    val reception: SosReceptionState = SosReceptionState(),
    val connection: PeerConnectionState = PeerConnectionState(),
)

private data class BaseState(
    val sosStatus: SosModeStatus,
    val scanStatus: ProximityScanStatus,
    val signals: List<NearbySosSignal>,
    val selectedPeerId: String?,
    val reception: SosReceptionState,
)

@HiltViewModel
class ProximityViewModel @Inject constructor(
    private val proximity: ManageEmergencyProximityUseCase,
) : ViewModel() {
    private val selectedPeerId = MutableStateFlow<String?>(null)
    private val selectedRole = MutableStateFlow<ProximityRole?>(null)

    private val baseState = combine(
        proximity.sosStatus,
        proximity.scanStatus,
        proximity.nearbySignals,
        selectedPeerId,
        proximity.sosReceptionState,
    ) { sosStatus, scanStatus, signals, selectedId, reception ->
        BaseState(sosStatus, scanStatus, signals, selectedId, reception)
    }

    val uiState: StateFlow<ProximityUiState> = combine(
        baseState,
        proximity.peerConnectionState,
        selectedRole,
    ) { base, connection, role ->
        val selectedSignal = base.signals.firstOrNull { it.peerId == base.selectedPeerId }
        ProximityUiState(
            role = role,
            sosStatus = base.sosStatus,
            scanStatus = base.scanStatus,
            signals = base.signals,
            selectedPeerId = base.selectedPeerId,
            selectedSignal = selectedSignal,
            signalLost = base.scanStatus == ProximityScanStatus.SCANNING &&
                base.selectedPeerId != null && selectedSignal == null,
            reception = base.reception,
            connection = connection,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProximityUiState(),
    )

    init {
        viewModelScope.launch {
            proximity.nearbySignals.collect { signals ->
                if (selectedRole.value == ProximityRole.RESCUER &&
                    selectedPeerId.value == null && signals.isNotEmpty()
                ) {
                    selectSignal(signals.first().peerId)
                }
            }
        }
    }

    fun selectRole(role: ProximityRole) {
        selectedRole.value = role
        when (role) {
            ProximityRole.SOS -> stopDetection()
            ProximityRole.RESCUER -> Unit
        }
    }

    fun clearRole() {
        if (selectedRole.value == ProximityRole.RESCUER) stopDetection()
        selectedRole.value = null
    }

    fun activateSos() = proximity.activateSos()
    fun deactivateSos() = proximity.deactivateSos()

    fun startDetection() {
        selectedPeerId.value = null
        proximity.startDetection()
    }

    fun stopDetection() {
        proximity.stopDetection()
        selectedPeerId.value = null
    }

    fun selectSignal(peerId: String) {
        if (selectedPeerId.value == peerId &&
            proximity.peerConnectionState.value.peerId == peerId
        ) return
        selectedPeerId.value = peerId
        proximity.confirmAndRange(peerId)
    }

    override fun onCleared() {
        proximity.stopDetection()
        super.onCleared()
    }
}
