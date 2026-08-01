package com.ayni.mobile.domain.proximity

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class ProximityPulseCue(
    val intervalMillis: Long,
    val toneFrequencyHz: Int,
    val toneDurationMillis: Int,
    val closeness: Double,
)

/**
 * Convierte una medida relativa en una señal auditiva continua. UWB tiene prioridad
 * cuando existe; RSSI sólo representa cercanía relativa y nunca se presenta como metros.
 */
fun proximityPulseCue(
    smoothedRssi: Double?,
    distanceMeters: Float?,
): ProximityPulseCue {
    val uwbCloseness = distanceMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?.let { 1.0 - (it / MAX_UWB_GUIDANCE_METERS).coerceIn(0f, 1f) }
    val rssiCloseness = smoothedRssi
        ?.takeIf(Double::isFinite)
        ?.let { ((it - FAR_RSSI_DBM) / (NEAR_RSSI_DBM - FAR_RSSI_DBM)).coerceIn(0.0, 1.0) }
    val closeness = uwbCloseness ?: rssiCloseness ?: 0.0

    return ProximityPulseCue(
        // La curva cuadrática acelera con claridad en el último tramo: a -50 dBm
        // produce ~164 ms, mientras que una señal débil conserva pausas largas.
        intervalMillis = (
            MIN_INTERVAL_MS + (1.0 - closeness).pow(2) * (MAX_INTERVAL_MS - MIN_INTERVAL_MS)
            ).roundToLong(),
        toneFrequencyHz = (MIN_TONE_HZ + closeness * (MAX_TONE_HZ - MIN_TONE_HZ)).roundToInt(),
        toneDurationMillis = (50 + closeness * 20).roundToInt(),
        closeness = closeness,
    )
}

private const val FAR_RSSI_DBM = -95.0
private const val NEAR_RSSI_DBM = -45.0
private const val MAX_UWB_GUIDANCE_METERS = 12f
private const val MIN_INTERVAL_MS = 140L
private const val MAX_INTERVAL_MS = 2_500L
private const val MIN_TONE_HZ = 550
private const val MAX_TONE_HZ = 2_100
