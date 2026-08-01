package com.ayni.mobile.domain.model

/**
 * Golpe válido del nodo estructural, sin dependencias de Room ni Android.
 * La lista entregada a Gemma siempre se ordena del más antiguo al más reciente.
 */
data class StructuralHitReading(
    val measuredAtEpochMs: Long,
    val frequencyHz: Double,
    val autocorrelationFrequencyHz: Double,
    val snrDb: Double,
    val periodicity: Double,
    val tiltChangeDeg: Double,
    val peakAccelerationMg: Double,
    val peakAngularVelocityDps: Double,
    val reason: String,
)
