package com.ayni.mobile.ui.structural

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.domain.model.SensorReading
import com.ayni.mobile.domain.model.StructuralResult
import com.ayni.mobile.domain.repository.SensorRepository
import com.ayni.mobile.domain.usecase.AnalyzeStructureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StructuralUiState {
    data object Capturing : StructuralUiState
    data object Analyzing : StructuralUiState
    data class Result(val result: StructuralResult) : StructuralUiState
    data class Error(val message: String) : StructuralUiState
}

private const val WAVEFORM_BUFFER_SIZE = 60 // ~5s a 12Hz, suficiente para el readout visual

@HiltViewModel
class StructuralViewModel @Inject constructor(
    private val analyzeStructureUseCase: AnalyzeStructureUseCase,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StructuralUiState>(StructuralUiState.Capturing)
    val uiState: StateFlow<StructuralUiState> = _uiState.asStateFlow()

    val sensorConnected: StateFlow<Boolean> = sensorRepository.isConnected

    private val _magnitudes = MutableStateFlow<List<Float>>(emptyList())
    val magnitudes: StateFlow<List<Float>> = _magnitudes.asStateFlow()

    private val _capturedImage = MutableStateFlow<ByteArray?>(null)
    val capturedImage: StateFlow<ByteArray?> = _capturedImage.asStateFlow()

    private var lastReading: SensorReading? = null

    init {
        viewModelScope.launch {
            sensorRepository.readings.collect { reading ->
                lastReading = reading
                _magnitudes.value = (_magnitudes.value + reading.magnitud).takeLast(WAVEFORM_BUFFER_SIZE)
            }
        }
    }

    fun onImageCaptured(imageBytes: ByteArray) {
        _uiState.value = StructuralUiState.Analyzing
        _capturedImage.value = imageBytes
        viewModelScope.launch {
            runCatching {
                analyzeStructureUseCase(imageBytes, lastReading)
            }.onSuccess { result ->
                _uiState.value = StructuralUiState.Result(result)
            }.onFailure {
                // AnalyzeStructureUseCase/GemmaAiRepository ya resuelven a un fallback
                // seguro internamente; esta rama solo cubre fallos verdaderamente
                // inesperados (p.ej. OOM al decodificar la imagen).
                _uiState.value = StructuralUiState.Error(
                    "No se pudo procesar la foto. Intenta de nuevo o evalúa manualmente."
                )
            }
        }
    }

    fun onSimulateAftershock() {
        sensorRepository.triggerSimulatedAftershock()
    }

    fun onNewAnalysis() {
        _uiState.value = StructuralUiState.Capturing
        _capturedImage.value = null
    }
}
