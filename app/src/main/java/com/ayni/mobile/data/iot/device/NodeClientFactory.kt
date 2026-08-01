package com.ayni.mobile.data.iot.device

/**
 * Fábrica del cliente de nodo. El cliente necesita su [SensorNodeClient.Listener] al
 * construirse (el ViewModel es el listener), así que no se puede inyectar el cliente
 * directamente: se inyecta esta fábrica y el ViewModel la invoca con `this`.
 *
 * Sustituir la implementación en di/IotModule (BleGateway ↔ FakeNodeClient) es lo que
 * permite ejecutar la app sin ESP32.
 */
fun interface NodeClientFactory {
    fun create(listener: SensorNodeClient.Listener): SensorNodeClient
}
