package com.ayni.mobile.domain.iot

enum class MeasurementMode {
    REST,
    HITS,
    SEISMIC,
}

enum class OperationalPhase(val wireValue: String) {
    CALIBRATING("calibrando"),
    RESTING("reposo"),
    READY("listo"),
    CAPTURING("capturando"),
    ANALYZING("analizando"),
    VALID("valido"),
    DISCARDED("descartado"),
    WATCHING_SEISMIC("vigilando_sismo"),
    SEISMIC_ACTIVE("sismo_activo"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromWire(value: String): OperationalPhase {
            val normalized = value.trim()
            return entries.firstOrNull {
                it != UNKNOWN && it.wireValue.equals(normalized, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}

enum class MeasurementGuidanceState {
    CALIBRATING,
    RESTING,
    READY_FOR_HIT,
    CAPTURING,
    ANALYZING,
    HIT_VALID,
    HIT_DISCARDED,
    WAITING_FOR_REST,
    SEISMIC_WATCHING,
    SEISMIC_ACTIVE,
    SYNCHRONIZING,
}

fun measurementGuidanceState(
    mode: MeasurementMode,
    phase: OperationalPhase,
): MeasurementGuidanceState {
    if (phase == OperationalPhase.CALIBRATING) {
        return MeasurementGuidanceState.CALIBRATING
    }
    return when (mode) {
        MeasurementMode.REST -> MeasurementGuidanceState.RESTING
        MeasurementMode.HITS -> when (phase) {
            OperationalPhase.READY -> MeasurementGuidanceState.READY_FOR_HIT
            OperationalPhase.CAPTURING -> MeasurementGuidanceState.CAPTURING
            OperationalPhase.ANALYZING -> MeasurementGuidanceState.ANALYZING
            OperationalPhase.VALID -> MeasurementGuidanceState.HIT_VALID
            OperationalPhase.DISCARDED -> MeasurementGuidanceState.HIT_DISCARDED
            OperationalPhase.RESTING -> MeasurementGuidanceState.WAITING_FOR_REST
            else -> MeasurementGuidanceState.SYNCHRONIZING
        }

        MeasurementMode.SEISMIC -> when (phase) {
            OperationalPhase.WATCHING_SEISMIC ->
                MeasurementGuidanceState.SEISMIC_WATCHING

            OperationalPhase.SEISMIC_ACTIVE ->
                MeasurementGuidanceState.SEISMIC_ACTIVE

            else -> MeasurementGuidanceState.SYNCHRONIZING
        }
    }
}

fun sameInstallationContext(
    firstDeviceId: String,
    firstInstallationId: String?,
    secondDeviceId: String,
    secondInstallationId: String?,
): Boolean =
    firstDeviceId == secondDeviceId &&
    firstInstallationId != null &&
        firstInstallationId == secondInstallationId

fun isMeasurementStateBeforeEvent(
    stateLastHitAtEpochMs: Long,
    eventStartedAtEpochMs: Long,
): Boolean = stateLastHitAtEpochMs <= eventStartedAtEpochMs

fun isMeasurementStateAfterEvent(
    stateFirstHitAtEpochMs: Long,
    eventEndedAtEpochMs: Long,
): Boolean = stateFirstHitAtEpochMs >= eventEndedAtEpochMs

data class DiscoveredNode(
    val name: String,
    val address: String,
    val rssi: Int,
)

enum class LinkState {
    IDLE,
    SCANNING,
    CONNECTING,
    DISCOVERING,
    READY,
    DISCONNECTED,
    ERROR,
}

/**
 * Red a la que está asociado el nodo. `null` con firmware anterior a 0.8, que
 * todavía no publica el grupo `wifi`: ausente y «sin enlace» son cosas
 * distintas y la interfaz no debe confundirlas.
 */
data class NodeWifiStatus(
    val ssid: String,
    val connected: Boolean,
    val ipAddress: String,
)

data class OperationalStatus(
    val deviceId: String,
    val displayName: String,
    val firmwareVersion: String,
    val sessionId: Long,
    val phase: OperationalPhase,
    val progress: Int,
    val validHits: Long,
    val attempts: Long,
    val targetHits: Long,
    val complete: Boolean,
    val acknowledged: Boolean,
    val measurementMode: MeasurementMode,
    val identityStored: Boolean = true,
    val identityWarning: Boolean = false,
    val wifi: NodeWifiStatus? = null,
)

data class SeismicEventRecord(
    val deviceId: String,
    val espSessionId: Long,
    val eventSequence: Long,
    val startedUptimeMs: Long,
    val durationSeconds: Double,
    val peakAccelerationMg: Double,
    val rmsAccelerationMg: Double,
    val dominantFrequencyHz: Double,
    val peakAngularVelocityDps: Double,
    val rollBeforeDeg: Double,
    val pitchBeforeDeg: Double,
    val rollAfterDeg: Double,
    val pitchAfterDeg: Double,
    val tiltChangeDeg: Double,
)

data class StructuralSnapshot(
    val deviceId: String,
    val sessionId: Long,
    val sequence: Long,
    val medianFrequencyHz: Double,
    val frequencyMadHz: Double,
    val lastFrequencyHz: Double,
    val snrDb: Double,
    val periodicity: Double,
    val usefulDurationSeconds: Double,
    val validHits: Long,
    val attempts: Long,
    val referenceRollDeg: Double,
    val referencePitchDeg: Double,
    val currentRollDeg: Double,
    val currentPitchDeg: Double,
    val tiltChangeDeg: Double,
    val orientationConfidence: Double,
    val peakAngularVelocityDps: Double,
    val peakDynamicAccelerationMg: Double,
    val integratedRotationXDeg: Double,
    val integratedRotationYDeg: Double,
    val integratedRotationZDeg: Double,
    val abruptMovement: Boolean,
    val accelerometerNoiseMg: Double,
    val gyroscopeBiasXDps: Double,
    val gyroscopeBiasYDps: Double,
    val gyroscopeBiasZDps: Double,
    val temperatureC: Double,
    val lastHitValid: Boolean,
    val fftFrequencyHz: Double,
    val autocorrelationFrequencyHz: Double,
    val resultReason: String,
)

data class MeasurementTrace(
    val deviceId: String,
    val sessionId: Long,
    val sequence: Long,
    val sampleRateHz: Int,
    val sampleCount: Int,
    val axis: String,
    val scaleMgPerLsb: Double,
    val meanRaw: Double,
    val samplesLittleEndian: ByteArray,
    val ignoredImpactSamples: Int,
    val analysisStartIndex: Int,
    val analysisEndIndex: Int,
    val initialWindowEndIndex: Int,
    val saturatedIndices: List<Int>,
    val saturatedIndicesTruncated: Boolean,
    val valid: Boolean,
    val reason: String,
    val rejectionCode: String,
    val rejectionStage: String,
    val rejectionScope: String,
    val diagnosticUnit: String,
    val diagnosticStartIndex: Int,
    val diagnosticEndIndex: Int,
    val measuredValue: Double,
    val minimumValue: Double?,
    val maximumValue: Double?,
    val noiseRmsMg: Double,
    val tailThresholdMg: Double,
    val triggerThresholdMg: Double,
    val fftFrequencyHz: Double,
    val autocorrelationFrequencyHz: Double,
    val snrDb: Double,
    val periodicity: Double,
    val initialRmsMg: Double,
)

data class ProtocolEvent(
    val name: String,
    val commandId: Long?,
    val result: String?,
    val detail: String,
    val sessionId: Long?,
    val sequence: Long?,
    val phase: String?,
)
