package com.ayni.mobile.domain.proximity

import java.util.ArrayDeque

/**
 * Suaviza RSSI y calcula solamente tendencia relativa. No intenta estimar distancia:
 * orientación, cuerpos, concreto y multitrayectoria invalidan una conversión estable.
 */
class RssiSignalTracker(
    private val smoothingFactor: Double = 0.22,
    private val trendWindowMs: Long = 2_500,
    private val trendThresholdDb: Double = 4.0,
) {
    private data class TimedRssi(val elapsedMs: Long, val value: Double)

    private val history = ArrayDeque<TimedRssi>()
    private var smoothed: Double? = null

    fun add(rssi: Int, elapsedMs: Long): RssiEstimate {
        val next = smoothed?.let { previous ->
            previous + smoothingFactor * (rssi - previous)
        } ?: rssi.toDouble()
        smoothed = next

        history.addLast(TimedRssi(elapsedMs, next))
        val oldestUsefulMs = elapsedMs - trendWindowMs
        while (history.size > 2 && history.elementAt(1).elapsedMs <= oldestUsefulMs) {
            history.removeFirst()
        }

        val reference = history.firstOrNull()
            ?.takeIf { elapsedMs - it.elapsedMs >= trendWindowMs }
        val trend = reference?.let {
            val delta = next - it.value
            when {
                delta >= trendThresholdDb -> ProximityTrend.APPROACHING
                delta <= -trendThresholdDb -> ProximityTrend.MOVING_AWAY
                else -> ProximityTrend.STABLE
            }
        } ?: ProximityTrend.UNKNOWN

        return RssiEstimate(
            smoothedRssi = next,
            level = when {
                next >= -67.0 -> ProximitySignalLevel.STRONG
                next >= -82.0 -> ProximitySignalLevel.MEDIUM
                else -> ProximitySignalLevel.WEAK
            },
            trend = trend,
        )
    }
}

data class RssiEstimate(
    val smoothedRssi: Double,
    val level: ProximitySignalLevel,
    val trend: ProximityTrend,
)

