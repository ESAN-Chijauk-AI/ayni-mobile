package com.ayni.mobile.domain.iot

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class RobustFrequencySummary(
    val medianHz: Double,
    val madHz: Double,
    val sampleCount: Int,
)

object RobustStatistics {
    fun summarize(frequenciesHz: List<Double>): RobustFrequencySummary {
        val sorted = frequenciesHz.filter { it.isFinite() && it > 0.0 }.sorted()
        val median = median(sorted)
        val mad = median(sorted.map { abs(it - median) }.sorted())
        return RobustFrequencySummary(
            medianHz = median,
            madHz = mad,
            sampleCount = sorted.size,
        )
    }

    fun medianValue(values: List<Double>): Double =
        median(values.filter { it.isFinite() }.sorted())

    fun circularMeanDegrees(values: List<Double>): Double {
        val finite = values.filter { it.isFinite() }
        if (finite.isEmpty()) return 0.0
        val sinMean = finite.sumOf { sin(Math.toRadians(it)) } / finite.size
        val cosMean = finite.sumOf { cos(Math.toRadians(it)) } / finite.size
        return Math.toDegrees(atan2(sinMean, cosMean))
    }

    private fun median(sorted: List<Double>): Double {
        if (sorted.isEmpty()) return 0.0
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
    }
}
