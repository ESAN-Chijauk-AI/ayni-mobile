package com.ayni.mobile.ui.structural

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.data.local.CompletedStructuralReport
import com.ayni.mobile.data.local.LastStructuralReportState
import com.ayni.mobile.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WAVEFORM_BUFFER_SIZE = 60

@HiltViewModel
class ReportsViewModel @Inject constructor(
    lastStructuralReportState: LastStructuralReportState,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    val lastReport: StateFlow<CompletedStructuralReport?> = lastStructuralReportState.lastReport
    val sensorConnected: StateFlow<Boolean> = sensorRepository.isConnected

    private val _magnitudes = MutableStateFlow<List<Float>>(emptyList())
    val magnitudes: StateFlow<List<Float>> = _magnitudes.asStateFlow()

    init {
        viewModelScope.launch {
            sensorRepository.readings.collect { reading ->
                _magnitudes.value = (_magnitudes.value + reading.magnitud).takeLast(WAVEFORM_BUFFER_SIZE)
            }
        }
    }

    fun onSimulateAftershock() {
        sensorRepository.triggerSimulatedAftershock()
    }
}
