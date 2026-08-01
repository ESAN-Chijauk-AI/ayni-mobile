package com.ayni.mobile.data.iot.device

import com.ayni.mobile.domain.iot.DiscoveredNode
import com.ayni.mobile.domain.iot.LinkState
import com.ayni.mobile.domain.iot.MeasurementTrace
import com.ayni.mobile.domain.iot.OperationalStatus
import com.ayni.mobile.domain.iot.ProtocolEvent
import com.ayni.mobile.domain.iot.SeismicEventRecord
import com.ayni.mobile.domain.iot.StructuralSnapshot

/**
 * Enlace con un nodo SISMO, independiente del transporte.
 *
 * Existe porque el ESP32 publica los mismos datos por dos caminos: BLE GATT
 * (`BleGateway`) y el servidor web local por Wi-Fi. El requisito de que la cola
 * de eventos sísmicos se drene «de la manera posible» obliga a que la sesión
 * hable con una interfaz y no con una implementación concreta.
 *
 * También permite desarrollar pestañas sin hardware: ver [FakeNodeClient].
 *
 * Contrato: todas las operaciones son asíncronas y sus resultados llegan por
 * [Listener]. Las implementaciones deben serializar las operaciones del
 * transporte; quien llama no coordina concurrencia.
 */
interface SensorNodeClient {

    interface Listener {
        fun onLinkState(state: LinkState, message: String)
        fun onNodesChanged(nodes: List<DiscoveredNode>)
        fun onOperationalStatus(status: OperationalStatus)
        fun onStructuralSnapshot(snapshot: StructuralSnapshot)
        fun onMeasurementTrace(trace: MeasurementTrace)
        fun onSeismicEvent(event: SeismicEventRecord)
        fun onProtocolEvent(event: ProtocolEvent)
        fun onProtocolDescription(json: String)
        fun onError(message: String)
    }

    fun startScan()

    fun connect(address: String)

    fun disconnect()

    /**
     * Encola un comando del protocolo. Devuelve el `commandId` generado, o
     * `null` si no hay enlace. El identificador hace la mutación idempotente:
     * el nodo recuerda los últimos que vio y no repite una duplicada.
     */
    fun sendCommand(name: String, argument: Long? = null): Long?

    /**
     * Variante con argumento de texto, para comandos cuyo valor no cabe en un
     * entero —hoy sólo `SET_WIFI`—. El nodo conserva el resto del envelope tal
     * cual, así que el argumento puede contener separadores.
     */
    fun sendTextCommand(name: String, argument: String): Long?

    fun close()
}
