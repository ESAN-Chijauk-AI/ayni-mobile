package com.ayni.mobile.ui.proximity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.domain.proximity.NearbySosSignal
import com.ayni.mobile.domain.proximity.ProximityScanStatus
import com.ayni.mobile.domain.proximity.SosModeStatus
import com.ayni.mobile.domain.usecase.ManageEmergencyProximityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProximityUiState(
    val sosStatus: SosModeStatus = SosModeStatus.INACTIVE,
    val scanStatus: ProximityScanStatus = ProximityScanStatus.IDLE,
    val signals: List<NearbySosSignal> = emptyList(),
    val selectedPeerId: String? = null,
    val selectedSignal: NearbySosSignal? = null,
)

@HiltViewModel
class ProximityViewModel @Inject constructor(
    private val proximity: ManageEmergencyProximityUseCase,
) : ViewModel() {
    private val selectedPeerId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProximityUiState> = combine(
        proximity.sosStatus,
        proximity.scanStatus,
        proximity.nearbySignals,
        selectedPeerId,
    ) { sosStatus, scanStatus, signals, selectedId ->
        ProximityUiState(
            sosStatus = sosStatus,
            scanStatus = scanStatus,
            signals = signals,
            selectedPeerId = selectedId,
            selectedSignal = signals.firstOrNull { it.peerId == selectedId },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProximityUiState(),
    )

    init {
        viewModelScope.launch {
            proximity.nearbySignals.collect { signals ->
                if (selectedPeerId.value == null && signals.isNotEmpty()) {
                    selectedPeerId.value = signals.first().peerId
                }
            }
        }
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
        selectedPeerId.value = peerId
    }

    override fun onCleared() {
        proximity.stopDetection()
        super.onCleared()
    }
}

