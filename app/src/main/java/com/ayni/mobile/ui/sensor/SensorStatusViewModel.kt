package com.ayni.mobile.ui.sensor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.domain.model.SensorReading
import com.ayni.mobile.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WAVEFORM_BUFFER_SIZE = 60

data class SensorStatusUiState(
    val connected: Boolean = false,
    val lastReading: SensorReading? = null,
    val magnitudes: List<Float> = emptyList(),
    val isSimulationMode: Boolean = true
)

@HiltViewModel
class SensorStatusViewModel @Inject constructor(
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SensorStatusUiState())
    val uiState: StateFlow<SensorStatusUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sensorRepository.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(connected = connected)
            }
        }
        viewModelScope.launch {
            sensorRepository.readings.collect { reading ->
                val current = _uiState.value
                _uiState.value = current.copy(
                    lastReading = reading,
                    magnitudes = (current.magnitudes + reading.magnitud).takeLast(WAVEFORM_BUFFER_SIZE),
                    isSimulationMode = reading.isSimulated
                )
            }
        }
    }

    fun onSimulateAftershock() {
        sensorRepository.triggerSimulatedAftershock()
    }

    fun onToggleConnection() {
        if (_uiState.value.connected) sensorRepository.disconnect() else sensorRepository.connect()
    }
}
