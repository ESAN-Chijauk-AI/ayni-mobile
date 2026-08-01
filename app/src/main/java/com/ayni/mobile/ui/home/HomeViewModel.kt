package com.ayni.mobile.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.data.ai.ModelPaths
import com.ayni.mobile.domain.repository.AiRepository
import com.ayni.mobile.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val aiReady: Boolean = false,
    val sensorConnected: Boolean = false,
    val modelFilePresent: Boolean = false,
    val isImportingModel: Boolean = false,
    val importError: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    sensorRepository: SensorRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val modelFilePresent = MutableStateFlow(ModelPaths.isModelPresent(context))
    private val isImportingModel = MutableStateFlow(false)
    private val importError = MutableStateFlow(false)

    // AiRepository.isReady no es reactivo (es un booleano simple del engine), así que se
    // sondea a intervalos cortos solo mientras aún no está listo; barato y suficiente
    // para reflejar "IA lista" en Home sin acoplar AiRepository a StateFlow por ahora.
    private val aiReady = MutableStateFlow(aiRepository.isReady)

    init {
        pollUntilReady()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        aiReady, sensorRepository.isConnected, modelFilePresent, isImportingModel, importError
    ) { ready, sensorConnected, modelPresent, importing, error ->
        HomeUiState(
            aiReady = ready,
            sensorConnected = sensorConnected,
            modelFilePresent = modelPresent,
            isImportingModel = importing,
            importError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            aiReady = aiReady.value,
            sensorConnected = false,
            modelFilePresent = modelFilePresent.value
        )
    )

    /**
     * El usuario eligió el archivo `.litertlm` con el selector de archivos del sistema
     * (ver HomeScreen). Lo copiamos al almacenamiento privado de la app y reintentamos
     * el warm-up del engine — sin esto la app se queda esperando un `adb push` manual,
     * que no todos pueden hacer desde el celular.
     */
    fun onModelFileSelected(uri: Uri) {
        if (isImportingModel.value) return
        isImportingModel.value = true
        importError.value = false
        viewModelScope.launch {
            val copied = withContext(Dispatchers.IO) { ModelPaths.copyFromUri(context, uri) }
            modelFilePresent.value = ModelPaths.isModelPresent(context)
            isImportingModel.value = false
            if (copied) {
                aiReady.value = false
                pollUntilReady()
            } else {
                importError.value = true
            }
        }
    }

    private fun pollUntilReady() {
        viewModelScope.launch {
            if (modelFilePresent.value) {
                runCatching { aiRepository.warmUp() }
            }
            while (!aiReady.value) {
                delay(300)
                aiReady.value = aiRepository.isReady
            }
        }
    }
}
