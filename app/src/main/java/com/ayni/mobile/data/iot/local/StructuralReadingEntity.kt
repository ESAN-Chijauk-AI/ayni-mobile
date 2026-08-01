package com.ayni.mobile.data.iot.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historial inmutable de telemetría usado para evaluar una ventana temporal.
 *
 * `StructuralStateEntity` conserva el último estado por sesión/secuencia y puede
 * sobrescribirse durante el reposo. Esta tabla agrega cada lectura para que una
 * consulta de cinco minutos represente una serie real y no una sola muestra.
 */
@Entity(
    tableName = "structural_readings",
    indices = [
        Index(value = ["deviceId", "installationId", "receivedAtEpochMs"]),
        Index(value = ["receivedAtEpochMs"]),
    ],
)
data class StructuralReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String,
    val installationId: String?,
    val receivedAtEpochMs: Long,
    val medianFrequencyHz: Double,
    val frequencyMadHz: Double,
    val snrDb: Double,
    val periodicity: Double,
    val tiltChangeDeg: Double,
    val orientationConfidence: Double,
    val peakAngularVelocityDps: Double,
    val peakDynamicAccelerationMg: Double,
    val abruptMovement: Boolean,
    val accelerometerNoiseMg: Double,
    val temperatureC: Double,
    val lastHitValid: Boolean,
    val resultReason: String,
)
