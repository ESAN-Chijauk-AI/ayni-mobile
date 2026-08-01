package com.ayni.mobile.domain.model

/**
 * @param isSimulated true mientras el dato venga de MockSensorRepository en vez de hardware
 * BLE real. La UI debe mostrar esto siempre explícito (nunca ocultar que es una simulación).
 */
data class SensorReading(
    val ax: Float,
    val ay: Float,
    val az: Float,
    val magnitud: Float,
    val ts: Long,
    val isSimulated: Boolean = true
)
