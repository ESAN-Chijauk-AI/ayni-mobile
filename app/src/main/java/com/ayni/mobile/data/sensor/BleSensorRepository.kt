package com.ayni.mobile.data.sensor

import com.ayni.mobile.domain.model.SensorReading
import com.ayni.mobile.domain.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * STUB — sin lógica real. Fuera de alcance de esta sesión (F4 despriorizado a pedido
 * del usuario: "las otras 2 primero"). Queda declarada la dependencia
 * `no.nordicsemi.android:ble` en el catálogo de versiones para no perder el paso
 * cuando se retome.
 *
 * TODO(ble): implementar con Nordic Android-BLE-Library sobre el ESP32+MPU6050:
 * - Escaneo por UUID de servicio del firmware de los compañeros.
 * - BleManager.connect(device) con auto-retry (lo trae la librería).
 * - Notificaciones de la característica de aceleración -> mapear bytes a SensorReading
 *   (isSimulated = false).
 * Para activar: cambiar el binding en di/SensorModule.kt de MockSensorRepository a
 * BleSensorRepository. Ningún ViewModel ni caso de uso necesita cambiar.
 */
class BleSensorRepository @Inject constructor() : SensorRepository {

    override val isConnected: StateFlow<Boolean> = MutableStateFlow(false)

    override val readings = emptyFlow<SensorReading>()

    override fun triggerSimulatedAftershock() {
        // No aplica a hardware real: una réplica real ya la reportaría el sensor.
    }

    override fun connect() {
        TODO("Implementar escaneo/conexión BLE con Nordic Android-BLE-Library (ESP32+MPU6050)")
    }

    override fun disconnect() {
        TODO("Implementar desconexión BLE")
    }
}
