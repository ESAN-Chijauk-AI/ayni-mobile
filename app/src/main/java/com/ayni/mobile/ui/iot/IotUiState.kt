package com.ayni.mobile.ui.iot

import com.ayni.mobile.domain.iot.DiscoveredNode
import com.ayni.mobile.domain.iot.LinkState
import com.ayni.mobile.domain.iot.MeasurementMode
import com.ayni.mobile.domain.iot.MeasurementTrace
import com.ayni.mobile.domain.iot.OperationalStatus
import com.ayni.mobile.domain.iot.StructuralSnapshot
import com.ayni.mobile.domain.model.StructuralSafetyAnalysis

sealed interface StructuralSafetyUiState {
    data object Idle : StructuralSafetyUiState
    data object Working : StructuralSafetyUiState

    data class InsufficientData(
        val validHitCount: Int,
        val requiredCount: Int,
    ) : StructuralSafetyUiState

    data class Success(
        val analysis: StructuralSafetyAnalysis,
        val validHitCount: Int,
        val maximumHitCount: Int,
    ) : StructuralSafetyUiState

    data class Failure(val message: String) : StructuralSafetyUiState
}

data class IotUiState(
    val permissionsGranted: Boolean = false,
    val linkState: LinkState = LinkState.IDLE,
    val linkMessage: String = "Busca un nodo para comenzar",
    val nodes: List<DiscoveredNode> = emptyList(),
    val connectedAddress: String? = null,
    val selectedDeviceId: String? = null,
    val operationalStatus: OperationalStatus? = null,
    val currentSnapshot: StructuralSnapshot? = null,
    val currentTrace: MeasurementTrace? = null,
    val protocolDescription: String = "",
    val measurementMode: MeasurementMode = MeasurementMode.REST,
    val selectedHitKeys: Set<String> = emptySet(),
    val selectedStateIds: List<String> = emptyList(),
    val selectedSeismicEventId: String? = null,
    val beforeStateId: String? = null,
    val afterStateId: String? = null,
    val identityConflict: String? = null,
    val structuralSafety: StructuralSafetyUiState = StructuralSafetyUiState.Idle,
    val message: String? = null,
    val error: String? = null,
)
