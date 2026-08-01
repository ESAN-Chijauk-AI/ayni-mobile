package com.ayni.mobile.domain.repository

import com.ayni.mobile.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Puerto hacia el sensor ESP32+MPU6050. Hoy bindeado a MockSensorRepository (F4 real
 * con Nordic BLE queda fuera de alcance de esta sesión, ver data/sensor/BleSensorRepository).
 */
interface SensorRepository {

    val isConnected: StateFlow<Boolean>

    /** Stream en vivo de aceleración, consumido por el readout signature y por el triage estructural. */
    val readings: Flow<SensorReading>

    /**
     * "Botón mágico" del demo: simula una réplica sísmica que sube el ruido de la señal
     * unos segundos, lo suficiente para que un análisis estructural en curso cambie de
     * veredicto en vivo. No-op en una implementación con hardware real.
     */
    fun triggerSimulatedAftershock()

    fun connect()
    fun disconnect()
}
