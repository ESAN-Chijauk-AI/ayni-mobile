package com.ayni.mobile.data.iot.ble

import com.ayni.mobile.domain.iot.MeasurementMode
import com.ayni.mobile.domain.iot.MeasurementTrace
import com.ayni.mobile.domain.iot.NodeWifiStatus
import com.ayni.mobile.domain.iot.OperationalPhase
import com.ayni.mobile.domain.iot.OperationalStatus
import com.ayni.mobile.domain.iot.ProtocolEvent
import com.ayni.mobile.domain.iot.SeismicEventRecord
import com.ayni.mobile.domain.iot.StructuralSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

object BleJsonParser {
    fun operationalStatus(json: String): OperationalStatus {
        val root = root(json)
        return OperationalStatus(
            deviceId = root.getString("device"),
            displayName = root.optString("name").ifBlank {
                root.getString("device")
            },
            firmwareVersion = root.optString("firmware"),
            sessionId = root.getLong("sessionId"),
            phase = OperationalPhase.fromWire(root.getString("phase")),
            progress = root.optInt("progress"),
            validHits = root.optLong("valid"),
            attempts = root.optLong("attempts"),
            targetHits = root.optLong("target"),
            complete = root.optBoolean("complete"),
            acknowledged = root.optBoolean("acked"),
            measurementMode = root.optString("mode").toMeasurementMode(),
            identityStored = root.optBoolean("identityStored", true),
            identityWarning = root.optBoolean("identityWarning", false),
            // Ausente con firmware anterior a 0.8.
            wifi = root.optJSONObject("wifi")?.let { wifi ->
                NodeWifiStatus(
                    ssid = wifi.optString("ssid"),
                    connected = wifi.optBoolean("up"),
                    ipAddress = wifi.optString("ip"),
                )
            },
        )
    }

    fun structuralSnapshot(json: String): StructuralSnapshot {
        val root = root(json)
        val vibration = root.getJSONObject("f")
        val orientation = root.getJSONObject("o")
        val movement = root.getJSONObject("m")
        val calibration = root.getJSONObject("c")
        val last = root.getJSONObject("last")
        val reference = orientation.getJSONArray("ref")
        val current = orientation.getJSONArray("now")
        val rotation = movement.getJSONArray("rot")
        val bias = calibration.getJSONArray("bias")

        return StructuralSnapshot(
            deviceId = root.getString("dev"),
            sessionId = root.getLong("sid"),
            sequence = root.optLong("seq"),
            medianFrequencyHz = vibration.number("med"),
            frequencyMadHz = vibration.number("mad"),
            lastFrequencyHz = vibration.number("last"),
            snrDb = vibration.number("snr"),
            periodicity = vibration.number("corr"),
            usefulDurationSeconds = vibration.number("dur"),
            validHits = vibration.optLong("ok"),
            attempts = vibration.optLong("try"),
            referenceRollDeg = reference.number(0),
            referencePitchDeg = reference.number(1),
            currentRollDeg = current.number(0),
            currentPitchDeg = current.number(1),
            tiltChangeDeg = orientation.number("tilt"),
            orientationConfidence = orientation.number("conf"),
            peakAngularVelocityDps = movement.number("gyro"),
            peakDynamicAccelerationMg = movement.number("acc"),
            integratedRotationXDeg = rotation.number(0),
            integratedRotationYDeg = rotation.number(1),
            integratedRotationZDeg = rotation.number(2),
            abruptMovement = movement.optBoolean("abrupt"),
            accelerometerNoiseMg = calibration.number("noise"),
            gyroscopeBiasXDps = bias.number(0),
            gyroscopeBiasYDps = bias.number(1),
            gyroscopeBiasZDps = bias.number(2),
            temperatureC = calibration.number("temp"),
            lastHitValid = last.optBoolean("ok"),
            fftFrequencyHz = last.number("fft"),
            autocorrelationFrequencyHz = last.number("auto"),
            resultReason = last.optString("why"),
        )
    }

    fun event(json: String): ProtocolEvent {
        val root = root(json)
        return ProtocolEvent(
            name = root.getString("event"),
            commandId = root.optionalLong("commandId"),
            result = root.optString("result").ifBlank { null },
            detail = root.optString("detail"),
            sessionId = root.optionalLong("sessionId"),
            sequence = root.optionalLong("sequence"),
            phase = root.optString("phase").ifBlank { null },
        )
    }

    fun eventName(json: String): String =
        root(json).getString("event")

    fun seismicEvent(json: String): SeismicEventRecord {
        val root = root(json)
        require(root.getString("event") == "seismic_event_completed") {
            "El evento no contiene un registro sísmico"
        }
        return SeismicEventRecord(
            deviceId = root.getString("device"),
            espSessionId = root.getLong("sessionId"),
            eventSequence = root.getLong("eventSequence"),
            startedUptimeMs = root.optLong("startedUptimeMs"),
            durationSeconds = root.number("durationS"),
            peakAccelerationMg = root.number("pgaMg"),
            rmsAccelerationMg = root.number("rmsMg"),
            dominantFrequencyHz = root.number("dominantHz"),
            peakAngularVelocityDps = root.number("gyroPeakDps"),
            rollBeforeDeg = root.number("rollBefore"),
            pitchBeforeDeg = root.number("pitchBefore"),
            rollAfterDeg = root.number("rollAfter"),
            pitchAfterDeg = root.number("pitchAfter"),
            tiltChangeDeg = root.number("tiltChange"),
        )
    }

    fun measurementTrace(json: String): MeasurementTrace {
        val root = root(json)
        require(root.getString("trace") == "hit_capture") {
            "El mensaje no contiene una captura de golpe"
        }
        val sampleRateHz = root.getInt("fs")
        val sampleCount = root.getInt("n")
        require(sampleRateHz == 100) {
            "Frecuencia de muestreo no soportada: $sampleRateHz Hz"
        }
        require(sampleCount in 1..2048) {
            "Cantidad de muestras inválida: $sampleCount"
        }
        require(root.getString("encoding") == "s16le-base64") {
            "Codificación de captura no soportada"
        }
        val samples = Base64.getDecoder().decode(root.getString("samples"))
        require(samples.size == sampleCount * Short.SIZE_BYTES) {
            "La captura declara $sampleCount puntos pero contiene ${samples.size} bytes"
        }
        val diagnostic = root.getJSONObject("diag")
        val saturatedJson = root.optJSONArray("sat") ?: JSONArray()
        val saturated = buildList {
            for (index in 0 until saturatedJson.length()) {
                add(saturatedJson.optInt(index))
            }
        }.filter { it in 0 until sampleCount }

        return MeasurementTrace(
            deviceId = root.getString("dev"),
            sessionId = root.getLong("sid"),
            sequence = root.getLong("seq"),
            sampleRateHz = sampleRateHz,
            sampleCount = sampleCount,
            axis = root.optString("axis", "X"),
            scaleMgPerLsb = root.number("scaleMg"),
            meanRaw = root.number("meanRaw"),
            samplesLittleEndian = samples,
            ignoredImpactSamples = root.optInt("ignoredBefore").coerceAtLeast(0),
            analysisStartIndex = root.optInt("analysisStart")
                .coerceIn(0, sampleCount),
            analysisEndIndex = root.optInt("analysisEnd")
                .coerceIn(0, sampleCount),
            initialWindowEndIndex = root.optInt("initialEnd")
                .coerceIn(0, sampleCount),
            saturatedIndices = saturated,
            saturatedIndicesTruncated = root.optBoolean("satTruncated"),
            valid = root.optBoolean("valid"),
            reason = root.optString("reason"),
            rejectionCode = diagnostic.optString("code", "UNKNOWN"),
            rejectionStage = diagnostic.optString("stage", "CAPTURE"),
            rejectionScope = diagnostic.optString("scope", "CAPTURE"),
            diagnosticUnit = diagnostic.optString("unit"),
            diagnosticStartIndex = diagnostic.optInt("start")
                .coerceIn(0, sampleCount),
            diagnosticEndIndex = diagnostic.optInt("end")
                .coerceIn(0, sampleCount),
            measuredValue = diagnostic.number("measured"),
            minimumValue = diagnostic.optionalDouble("min"),
            maximumValue = diagnostic.optionalDouble("max"),
            noiseRmsMg = root.number("noiseMg"),
            tailThresholdMg = root.number("tailThresholdMg"),
            triggerThresholdMg = root.number("triggerMg"),
            fftFrequencyHz = root.number("fftHz"),
            autocorrelationFrequencyHz = root.number("autoHz"),
            snrDb = root.number("snrDb"),
            periodicity = root.number("periodicity"),
            initialRmsMg = root.number("rmsInitialMg"),
        )
    }

    private fun root(json: String): JSONObject =
        JSONObject(json).also {
            require(it.optInt("v") == BleProtocol.VERSION) {
                "Versión JSON no soportada: ${it.optInt("v")}"
            }
        }

    private fun JSONObject.number(name: String): Double =
        opt(name).asDouble()

    private fun JSONArray.number(index: Int): Double =
        opt(index).asDouble()

    private fun Any?.asDouble(): Double =
        when (this) {
            is Number -> toDouble()
            is String -> toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    private fun JSONObject.optionalLong(name: String): Long? =
        if (has(name) && !isNull(name)) optLong(name) else null

    private fun JSONObject.optionalDouble(name: String): Double? =
        if (has(name) && !isNull(name)) number(name) else null

    private fun String.toMeasurementMode(): MeasurementMode =
        when (uppercase()) {
            "HITS", "GOLPES" -> MeasurementMode.HITS
            "SEISMIC", "SISMICO", "SÍSMICO" -> MeasurementMode.SEISMIC
            else -> MeasurementMode.REST
        }
}
