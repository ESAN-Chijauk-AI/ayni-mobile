package com.ayni.mobile.ui.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayni.mobile.domain.model.MedicalResult
import com.ayni.mobile.domain.usecase.TriageMedicalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MedicalUiState {
    data class Input(val selectedChips: Set<String> = emptySet(), val freeText: String = "") : MedicalUiState
    data object Analyzing : MedicalUiState
    data class Result(val result: MedicalResult) : MedicalUiState
    data class Error(val message: String) : MedicalUiState
}

/** Lesiones comunes post-sismo, para preferir botones sobre teclear (§3/F3). */
val COMMON_INJURIES = listOf(
    "Sangrado", "Fractura", "Quemadura", "Golpe en la cabeza",
    "Aplastamiento", "Dificultad para respirar", "Inconsciente"
)

@HiltViewModel
class MedicalViewModel @Inject constructor(
    private val triageMedicalUseCase: TriageMedicalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MedicalUiState>(MedicalUiState.Input())
    val uiState: StateFlow<MedicalUiState> = _uiState.asStateFlow()

    private val _selectedImage = MutableStateFlow<ByteArray?>(null)
    val selectedImage: StateFlow<ByteArray?> = _selectedImage.asStateFlow()

    fun onImageSelected(imageBytes: ByteArray?) {
        _selectedImage.value = imageBytes
    }

    fun onToggleChip(label: String) {
        val current = _uiState.value as? MedicalUiState.Input ?: return
        val updated = if (label in current.selectedChips) {
            current.selectedChips - label
        } else {
            current.selectedChips + label
        }
        _uiState.value = current.copy(selectedChips = updated)
    }

    fun onFreeTextChange(text: String) {
        val current = _uiState.value as? MedicalUiState.Input ?: return
        _uiState.value = current.copy(freeText = text)
    }

    fun onSubmit() {
        val current = _uiState.value as? MedicalUiState.Input ?: return
        val description = buildDescription(current.selectedChips, current.freeText)
        if (description.isBlank()) return

        _uiState.value = MedicalUiState.Analyzing
        viewModelScope.launch {
            runCatching {
                triageMedicalUseCase(description, _selectedImage.value)
            }.onSuccess { result ->
                _uiState.value = MedicalUiState.Result(result)
            }.onFailure {
                _uiState.value = MedicalUiState.Error(
                    "No se pudo evaluar. Intenta de nuevo o busca ayuda profesional."
                )
            }
        }
    }

    fun onNewAnalysis() {
        _uiState.value = MedicalUiState.Input()
        _selectedImage.value = null
    }

    private fun buildDescription(chips: Set<String>, freeText: String): String {
        val parts = mutableListOf<String>()
        if (chips.isNotEmpty()) parts += chips.joinToString(", ")
        if (freeText.isNotBlank()) parts += freeText.trim()
        return parts.joinToString(". ")
    }
}
