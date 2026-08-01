package com.ayni.mobile.domain.model

data class MedicalResult(
    val prioridad: MedicalPriority,
    val primerosAuxilios: List<String>
)
