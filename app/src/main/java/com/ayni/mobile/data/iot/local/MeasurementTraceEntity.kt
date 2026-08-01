package com.ayni.mobile.data.iot.local

import androidx.room.Entity
import androidx.room.Index
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(
    tableName = "measurement_traces",
    primaryKeys = ["deviceId", "sessionId", "sequence"],
    indices = [
        Index("deviceId"),
        Index("receivedAtEpochMs"),
        Index("installationId"),
    ],
)
data class MeasurementTraceEntity(
    val deviceId: String,
    val sessionId: Long,
    val sequence: Long,
    val installationId: String?,
    val receivedAtEpochMs: Long,
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
    val saturatedIndicesCsv: String,
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
) {
    fun dynamicSamplesMg(): List<Float> {
        val available = samplesLittleEndian.size / Short.SIZE_BYTES
        val count = sampleCount.coerceIn(0, available)
        val buffer = ByteBuffer.wrap(samplesLittleEndian)
            .order(ByteOrder.LITTLE_ENDIAN)
        return List(count) {
            ((buffer.short.toDouble() - meanRaw) * scaleMgPerLsb).toFloat()
        }
    }

    fun saturatedIndices(): Set<Int> =
        saturatedIndicesCsv
            .split(',')
            .mapNotNull { it.toIntOrNull() }
            .filterTo(linkedSetOf()) { it in 0 until sampleCount }

    fun stableKey(): String = "$deviceId|$sessionId|$sequence"
}
