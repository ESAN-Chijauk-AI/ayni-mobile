package com.ayni.mobile.data.ai

import kotlinx.serialization.Serializable

/**
 * DTOs del JSON crudo que devuelve Gemma. Separados de domain.model a propósito:
 * domain no debería conocer la forma exacta del contrato de red/inferencia (nombres
 * de campo en español, mayúsculas, etc.) — TriageJsonParser hace el mapeo.
 */
@Serializable
data class StructuralJson(
    val veredicto: String,
    val razon: String,
    val accion: String
)

@Serializable
data class MedicalJson(
    val prioridad: String,
    val primeros_auxilios: List<String>
)
