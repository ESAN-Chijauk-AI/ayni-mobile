package com.ayni.mobile.data.iot.local

import com.ayni.mobile.domain.iot.MeasurementTrace
import com.ayni.mobile.domain.iot.StructuralSnapshot

/**
 * Mappers dominio → entidad Room.
 *
 * En ProtoEstados vivían como `asEntity` dentro de las data class del dominio.
 * En ayni el dominio es Kotlin puro (sin imports de Room), así que la conversión
 * baja a la capa de datos como funciones de extensión.
 */

fun StructuralSnapshot.toEntity(
    receivedAtEpochMs: Long = System.currentTimeMillis(),
    installationId: String? = null,
): StructuralStateEntity =
    StructuralStateEntity(
        deviceId = deviceId,
        sessionId = sessionId,
        sequence = sequence,
        installationId = installationId,
        receivedAtEpochMs = receivedAtEpochMs,
        // El snapshot estructural del firmware no transporta terminación ni
        // confirmación: `complete`/`acked` sólo existen en el estado operativo.
        complete = false,
        espAckedBeforeReceive = false,
        medianFrequencyHz = medianFrequencyHz,
        frequencyMadHz = frequencyMadHz,
        lastFrequencyHz = lastFrequencyHz,
        snrDb = snrDb,
        periodicity = periodicity,
        usefulDurationSeconds = usefulDurationSeconds,
        validHits = validHits,
        attempts = attempts,
        referenceRollDeg = referenceRollDeg,
        referencePitchDeg = referencePitchDeg,
        currentRollDeg = currentRollDeg,
        currentPitchDeg = currentPitchDeg,
        tiltChangeDeg = tiltChangeDeg,
        orientationConfidence = orientationConfidence,
        peakAngularVelocityDps = peakAngularVelocityDps,
        peakDynamicAccelerationMg = peakDynamicAccelerationMg,
        integratedRotationXDeg = integratedRotationXDeg,
        integratedRotationYDeg = integratedRotationYDeg,
        integratedRotationZDeg = integratedRotationZDeg,
        abruptMovement = abruptMovement,
        accelerometerNoiseMg = accelerometerNoiseMg,
        gyroscopeBiasXDps = gyroscopeBiasXDps,
        gyroscopeBiasYDps = gyroscopeBiasYDps,
        gyroscopeBiasZDps = gyroscopeBiasZDps,
        temperatureC = temperatureC,
        lastHitValid = lastHitValid,
        fftFrequencyHz = fftFrequencyHz,
        autocorrelationFrequencyHz = autocorrelationFrequencyHz,
        resultReason = resultReason,
    )

fun MeasurementTrace.toEntity(
    receivedAtEpochMs: Long = System.currentTimeMillis(),
    installationId: String? = null,
): MeasurementTraceEntity =
    MeasurementTraceEntity(
        deviceId = deviceId,
        sessionId = sessionId,
        sequence = sequence,
        installationId = installationId,
        receivedAtEpochMs = receivedAtEpochMs,
        sampleRateHz = sampleRateHz,
        sampleCount = sampleCount,
        axis = axis,
        scaleMgPerLsb = scaleMgPerLsb,
        meanRaw = meanRaw,
        samplesLittleEndian = samplesLittleEndian.copyOf(),
        ignoredImpactSamples = ignoredImpactSamples,
        analysisStartIndex = analysisStartIndex,
        analysisEndIndex = analysisEndIndex,
        initialWindowEndIndex = initialWindowEndIndex,
        saturatedIndicesCsv = saturatedIndices.joinToString(","),
        saturatedIndicesTruncated = saturatedIndicesTruncated,
        valid = valid,
        reason = reason,
        rejectionCode = rejectionCode,
        rejectionStage = rejectionStage,
        rejectionScope = rejectionScope,
        diagnosticUnit = diagnosticUnit,
        diagnosticStartIndex = diagnosticStartIndex,
        diagnosticEndIndex = diagnosticEndIndex,
        measuredValue = measuredValue,
        minimumValue = minimumValue,
        maximumValue = maximumValue,
        noiseRmsMg = noiseRmsMg,
        tailThresholdMg = tailThresholdMg,
        triggerThresholdMg = triggerThresholdMg,
        fftFrequencyHz = fftFrequencyHz,
        autocorrelationFrequencyHz = autocorrelationFrequencyHz,
        snrDb = snrDb,
        periodicity = periodicity,
        initialRmsMg = initialRmsMg,
    )
