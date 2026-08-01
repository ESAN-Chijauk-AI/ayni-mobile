package com.ayni.mobile.data.iot.device

import com.ayni.mobile.domain.iot.DiscoveredNode
import com.ayni.mobile.domain.iot.LinkState
import com.ayni.mobile.domain.iot.MeasurementMode
import com.ayni.mobile.domain.iot.NodeWifiStatus
import com.ayni.mobile.domain.iot.OperationalPhase
import com.ayni.mobile.domain.iot.OperationalStatus
import com.ayni.mobile.domain.iot.ProtocolEvent
import com.ayni.mobile.domain.iot.StructuralSnapshot

/**
 * Nodo simulado. Existe porque hay menos ESP32 que personas: permite construir
 * y revisar pestañas sin hardware.
 *
 * Deliberadamente tonto. No reproduce la máquina de estados del firmware ni el
 * detector STA/LTA: responde lo mínimo para que la interfaz tenga datos con
 * forma válida. Si necesitas comprobar una regla del algoritmo, escribe un test
 * de la función pura correspondiente en `domain/`, no amplíes esto.
 *
 * Para usarlo, cambia el binding en di/IotModule.kt para que la fábrica devuelva
 * FakeNodeClient(listener) en vez de BleGateway(context, listener).
 */
class FakeNodeClient(
    private val listener: SensorNodeClient.Listener,
    private val deviceId: String = "a4af5954-5ba7-4ee2-a88a-d33d9340d57f",
    private val displayName: String = "SISMO-NODO-FAKE",
) : SensorNodeClient {

    private var sequence = 0L
    private var mode = MeasurementMode.REST
    private var commandId = 0L
    private var fakeWifiSsid = "Red-Simulada"

    override fun startScan() {
        listener.onLinkState(LinkState.SCANNING, "Buscando nodos simulados…")
        listener.onNodesChanged(
            listOf(
                DiscoveredNode(name = displayName, address = FAKE_ADDRESS, rssi = -47),
            ),
        )
        listener.onLinkState(LinkState.IDLE, "Búsqueda terminada")
    }

    override fun connect(address: String) {
        listener.onLinkState(LinkState.CONNECTING, "Conectando con el nodo simulado…")
        listener.onLinkState(LinkState.READY, "Nodo simulado conectado")
        listener.onProtocolDescription("""{"version":1,"firmware":"0.6.0-fake"}""")
        emitStatus(OperationalPhase.RESTING)
    }

    override fun disconnect() {
        listener.onLinkState(LinkState.DISCONNECTED, "Nodo simulado desconectado")
    }

    override fun sendTextCommand(name: String, argument: String): Long? {
        val id = ++commandId
        if (name == "SET_WIFI") {
            fakeWifiSsid = argument.substringBefore('|')
            emitStatus(phaseFor(mode))
            listener.onProtocolEvent(event("wifi_changed", id, fakeWifiSsid))
        }
        return id
    }

    override fun sendCommand(name: String, argument: Long?): Long? {
        val id = ++commandId
        when (name) {
            "SET_MODE" -> {
                mode = when (argument) {
                    1L -> MeasurementMode.HITS
                    2L -> MeasurementMode.SEISMIC
                    else -> MeasurementMode.REST
                }
                emitStatus(phaseFor(mode))
                listener.onProtocolEvent(event("mode_changed", id, mode.name))
            }

            "RESET_ALGORITHM" -> {
                sequence = 0
                emitStatus(OperationalPhase.CALIBRATING)
                listener.onProtocolEvent(event("command_result", id, "nueva sesion calibrando"))
            }

            "GET_STATUS" -> emitStatus(phaseFor(mode))

            "GET_STATE" -> if (sequence > 0) emitSnapshot()

            else -> listener.onProtocolEvent(event("command_result", id, "ok"))
        }
        return id
    }

    override fun close() = Unit

    /** Simula un golpe válido para probar la pestaña de medición. */
    fun emitHit(frequencyHz: Double = 18.0) {
        sequence++
        emitStatus(OperationalPhase.VALID)
        emitSnapshot(frequencyHz)
    }

    private fun phaseFor(mode: MeasurementMode) = when (mode) {
        MeasurementMode.REST -> OperationalPhase.RESTING
        MeasurementMode.HITS -> OperationalPhase.READY
        MeasurementMode.SEISMIC -> OperationalPhase.WATCHING_SEISMIC
    }

    private fun emitStatus(phase: OperationalPhase) {
        listener.onOperationalStatus(
            OperationalStatus(
                deviceId = deviceId,
                displayName = displayName,
                firmwareVersion = "0.8.0-fake",
                sessionId = FAKE_SESSION_ID,
                phase = phase,
                progress = 100,
                validHits = sequence,
                attempts = sequence,
                targetHits = 0,
                complete = false,
                acknowledged = false,
                measurementMode = mode,
                wifi = NodeWifiStatus(
                    ssid = fakeWifiSsid,
                    connected = true,
                    ipAddress = "192.168.1.42",
                ),
            ),
        )
    }

    private fun emitSnapshot(frequencyHz: Double = 18.0) {
        listener.onStructuralSnapshot(
            StructuralSnapshot(
                deviceId = deviceId,
                sessionId = FAKE_SESSION_ID,
                sequence = sequence,
                medianFrequencyHz = frequencyHz,
                frequencyMadHz = 0.06,
                lastFrequencyHz = frequencyHz,
                snrDb = 18.4,
                periodicity = 0.82,
                usefulDurationSeconds = 2.73,
                validHits = sequence,
                attempts = sequence,
                referenceRollDeg = 89.73,
                referencePitchDeg = -0.42,
                currentRollDeg = 90.01,
                currentPitchDeg = -0.38,
                tiltChangeDeg = 0.28,
                orientationConfidence = 0.98,
                peakAngularVelocityDps = 12.6,
                peakDynamicAccelerationMg = 184.2,
                integratedRotationXDeg = 0.21,
                integratedRotationYDeg = -0.08,
                integratedRotationZDeg = 0.04,
                abruptMovement = false,
                accelerometerNoiseMg = 1.7,
                gyroscopeBiasXDps = 0.03,
                gyroscopeBiasYDps = -0.02,
                gyroscopeBiasZDps = 0.01,
                temperatureC = 26.4,
                lastHitValid = true,
                fftFrequencyHz = frequencyHz,
                autocorrelationFrequencyHz = frequencyHz - 0.14,
                resultReason = "Valida; SNR 18.4 dB; 2.73 s",
            ),
        )
    }

    private fun event(name: String, id: Long, detail: String) = ProtocolEvent(
        name = name,
        commandId = id,
        result = "ok",
        detail = detail,
        sessionId = FAKE_SESSION_ID,
        sequence = sequence,
        phase = null,
    )

    private companion object {
        const val FAKE_ADDRESS = "00:00:00:00:00:00"
        const val FAKE_SESSION_ID = 389123L
    }
}
