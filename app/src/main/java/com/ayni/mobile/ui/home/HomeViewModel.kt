package com.ayni.mobile.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.data.ai.ModelPaths
import com.ayni.mobile.domain.repository.AiRepository
import com.ayni.mobile.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val aiReady: Boolean = false,
    val sensorConnected: Boolean = false,
    val modelFilePresent: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    sensorRepository: SensorRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val modelFilePresent = MutableStateFlow(ModelPaths.isModelPresent(context))

    // AiRepository.isReady no es reactivo (es un booleano simple del engine), así que se
    // sondea a intervalos cortos solo mientras aún no está listo; barato y suficiente
    // para reflejar "IA lista" en Home sin acoplar AiRepository a StateFlow por ahora.
    private val aiReady = MutableStateFlow(aiRepository.isReady)

    init {
        viewModelScope.launch {
            while (!aiReady.value) {
                delay(300)
                aiReady.value = aiRepository.isReady
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        aiReady,
        sensorRepository.isConnected,
        modelFilePresent
    ) { ready, sensorConnected, modelPresent ->
        HomeUiState(
            aiReady = ready,
            sensorConnected = sensorConnected,
            modelFilePresent = modelPresent
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
}
