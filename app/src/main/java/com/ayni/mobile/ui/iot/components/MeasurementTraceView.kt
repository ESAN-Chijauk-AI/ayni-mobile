package com.ayni.mobile.ui.iot.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayni.mobile.R
import com.ayni.mobile.data.iot.local.MeasurementTraceEntity
import com.ayni.mobile.domain.iot.HitDiagnosticKind
import com.ayni.mobile.domain.iot.decimal
import kotlin.math.abs
import kotlin.math.max

@Composable
internal fun MeasurementTraceCard(
    trace: MeasurementTraceEntity,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val diagnosticKind = remember(trace.rejectionCode) {
        HitDiagnosticKind.fromWire(trace.rejectionCode)
    }
    val historyShift =
        trace.valid && diagnosticKind == HitDiagnosticKind.FREQUENCY_SHIFT
    val resultColor = when {
        historyShift -> MaterialTheme.colorScheme.tertiary
        trace.valid -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    val darkPalette = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        resultColor.copy(
                            alpha = if (darkPalette) 0.68f else 0.48f,
                        ),
                        MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = if (darkPalette) 0.68f else 0.35f,
                        ),
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = lerp(
                if (darkPalette) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
                resultColor,
                if (darkPalette) 0.085f else 0.055f,
            ),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (darkPalette) 4.dp else 2.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Captura #${trace.sequence} · ${trace.sampleRateHz} Hz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (historyShift) {
                            "Válida · cambio respecto al historial"
                        } else if (trace.valid) {
                            "Aceptada por todas las etapas"
                        } else {
                            "Descartada en ${stageLabel(trace.rejectionStage)}"
                        },
                        color = resultColor,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(onClick = onToggleExpanded) {
                    Text(
                        stringResource(
                            if (expanded) {
                                R.string.measure_diagnostic_hide_details
                            } else {
                                R.string.measure_diagnostic_show_details
                            },
                        ),
                    )
                }
            }

            PlainLanguageDiagnostic(
                kind = diagnosticKind,
                resultColor = resultColor,
            )

            if (expanded) {
                MeasurementTraceDetails(
                    trace = trace,
                    diagnosticKind = diagnosticKind,
                )
            } else {
                DiagnosticStageStrip(
                    activeStage = trace.rejectionStage,
                    valid = trace.valid,
                    warning = historyShift,
                )
            }
        }
    }
}

@Composable
private fun PlainLanguageDiagnostic(
    kind: HitDiagnosticKind,
    resultColor: Color,
) {
    val copy = diagnosticCopyResources(kind)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = resultColor.copy(alpha = 0.09f),
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.measure_diagnostic_what_happened),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(copy.summary),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                stringResource(R.string.measure_diagnostic_next_step),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(copy.action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MeasurementTraceDetails(
    trace: MeasurementTraceEntity,
    diagnosticKind: HitDiagnosticKind = HitDiagnosticKind.fromWire(trace.rejectionCode),
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiagnosticStageStrip(
            activeStage = trace.rejectionStage,
            valid = trace.valid,
            warning =
                trace.valid && diagnosticKind == HitDiagnosticKind.FREQUENCY_SHIFT,
        )
        DiagnosticMeasurement(
            trace = trace,
            kind = diagnosticKind,
        )
        GraphExplanation(
            trace = trace,
            kind = diagnosticKind,
        )
        TraceWaveform(trace)
        TraceLegend(trace)
        Text(
            "El eje X mostrado es exactamente el que analiza el firmware. " +
                "${trace.ignoredImpactSamples} muestras " +
                "(${decimal(trace.ignoredImpactSamples * 1000.0 / trace.sampleRateHz, 0)} ms) " +
                "del impacto se ignoraron antes de comenzar este búfer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "FFT ${decimal(trace.fftFrequencyHz)} Hz · " +
                "autocorrelación ${decimal(trace.autocorrelationFrequencyHz)} Hz · " +
                "SNR ${decimal(trace.snrDb)} dB · " +
                "periodicidad ${decimal(trace.periodicity)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DiagnosticStageStrip(
    activeStage: String,
    valid: Boolean,
    warning: Boolean,
) {
    val stages = listOf(
        "CAPTURE" to "Captura",
        "SIGNAL_QUALITY" to "Señal",
        "USABLE_WINDOW" to "Ventana",
        "SPECTRUM" to "Espectro",
        "AUTOCORRELATION" to "Período",
        "CONSISTENCY" to "Coherencia",
        "HISTORY" to "Historial",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        stages.forEach { (id, label) ->
            val active = (!valid || warning) && id == activeStage
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when {
                    active && warning -> MaterialTheme.colorScheme.tertiaryContainer
                    active -> MaterialTheme.colorScheme.errorContainer
                    valid -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        active && warning -> MaterialTheme.colorScheme.onTertiaryContainer
                        active -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun DiagnosticMeasurement(
    trace: MeasurementTraceEntity,
    kind: HitDiagnosticKind,
) {
    val historyShift =
        trace.valid && kind == HitDiagnosticKind.FREQUENCY_SHIFT
    val copy = diagnosticCopyResources(kind)
    val unit = trace.diagnosticUnit.takeIf { it.isNotBlank() }
        ?.let { " $it" }
        .orEmpty()
    val measured = decimal(trace.measuredValue)
    val comparison = when {
        trace.minimumValue != null && trace.maximumValue != null ->
            stringResource(
                R.string.measure_diagnostic_measured_range,
                measured,
                unit,
                decimal(trace.minimumValue),
                decimal(trace.maximumValue),
            )

        trace.minimumValue != null ->
            stringResource(
                R.string.measure_diagnostic_measured_minimum,
                measured,
                unit,
                decimal(trace.minimumValue),
            )

        trace.maximumValue != null ->
            stringResource(
                R.string.measure_diagnostic_measured_maximum,
                measured,
                unit,
                decimal(trace.maximumValue),
            )

        else ->
            stringResource(
                R.string.measure_diagnostic_measured_no_limit,
                measured,
                unit,
            )
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            historyShift -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
            trace.valid -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
        },
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                stringResource(
                    if (historyShift) {
                        R.string.measure_diagnostic_warning_title
                    } else {
                        R.string.measure_diagnostic_technical_title
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(copy.technical),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "$comparison · ${scopeLabel(trace.rejectionScope)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                stringResource(
                    R.string.measure_diagnostic_code_stage,
                    trace.rejectionCode,
                    stageLabel(trace.rejectionStage),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trace.reason.isNotBlank()) {
                Text(
                    trace.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GraphExplanation(
    trace: MeasurementTraceEntity,
    kind: HitDiagnosticKind,
) {
    val explanation = when {
        kind == HitDiagnosticKind.FREQUENCY_SHIFT ->
            R.string.measure_diagnostic_graph_history

        trace.valid ->
            R.string.measure_diagnostic_graph_accepted

        trace.rejectionScope == "SAMPLES" ->
            R.string.measure_diagnostic_graph_samples

        trace.rejectionScope == "RANGE" ->
            R.string.measure_diagnostic_graph_range

        else ->
            R.string.measure_diagnostic_graph_capture
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.measure_diagnostic_graph_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(explanation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TraceWaveform(trace: MeasurementTraceEntity) {
    val samples = remember(
        trace.deviceId,
        trace.sessionId,
        trace.sequence,
        trace.samplesLittleEndian.contentHashCode(),
    ) {
        trace.dynamicSamplesMg()
    }
    val saturated = remember(trace.saturatedIndicesCsv, trace.sampleCount) {
        trace.saturatedIndices()
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val tailColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    val rejectionColor = MaterialTheme.colorScheme.error
    val noiseColor = MaterialTheme.colorScheme.tertiary
    val maxMagnitude = remember(samples, trace.tailThresholdMg) {
        max(
            1f,
            max(
                samples.maxOfOrNull { abs(it) } ?: 1f,
                (trace.tailThresholdMg * 1.4).toFloat(),
            ),
        ) * 1.08f
    }
    val durationSeconds = trace.sampleCount.toDouble() / trace.sampleRateHz
    val darkPalette = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val plotBackground = if (darkPalette) {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f)
    }

    Text(
        "Aceleración dinámica [mg] · escala ±${decimal(maxMagnitude.toDouble(), 1)} mg",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(
                plotBackground,
                RoundedCornerShape(14.dp),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = if (darkPalette) 0.78f else 0.48f,
                ),
                RoundedCornerShape(14.dp),
            )
            .semantics {
                contentDescription =
                    "Onda de ${trace.sampleCount} muestras a ${trace.sampleRateHz} hercios. " +
                        if (trace.valid) {
                            "Captura aceptada."
                        } else {
                            "Captura descartada: ${trace.reason}."
                        }
            },
    ) {
        if (samples.size < 2) return@Canvas
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val plotHeight = bottom - top
        val lastIndex = samples.lastIndex.coerceAtLeast(1)

        fun x(index: Int): Float =
            index.coerceIn(0, lastIndex).toFloat() / lastIndex * size.width

        fun y(value: Float): Float =
            top + (maxMagnitude - value) / (2f * maxMagnitude) * plotHeight

        repeat(5) { index ->
            val fraction = index / 4f
            drawLine(
                color = gridColor,
                start = Offset(fraction * size.width, top),
                end = Offset(fraction * size.width, bottom),
                strokeWidth = 1.dp.toPx(),
            )
        }
        repeat(5) { index ->
            val fraction = index / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, top + fraction * plotHeight),
                end = Offset(size.width, top + fraction * plotHeight),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val analysisEnd = trace.analysisEndIndex.coerceIn(0, samples.size)
        if (analysisEnd < samples.size) {
            drawRect(
                color = tailColor,
                topLeft = Offset(x(analysisEnd), top),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width - x(analysisEnd),
                    height = plotHeight,
                ),
            )
        }

        if (!trace.valid && trace.rejectionScope != "SAMPLES") {
            val diagnosticStart = when (trace.rejectionScope) {
                "HISTORY" -> -1
                "CAPTURE" -> trace.analysisStartIndex
                else -> trace.diagnosticStartIndex
            }
            val diagnosticEnd = when (trace.rejectionScope) {
                "HISTORY" -> -1
                "CAPTURE" -> trace.analysisEndIndex
                else -> trace.diagnosticEndIndex
            }
            if (diagnosticStart >= 0 && diagnosticEnd > diagnosticStart) {
                val left = x(diagnosticStart)
                val right = x(diagnosticEnd.coerceAtMost(lastIndex))
                drawRect(
                    color = rejectionColor.copy(alpha = 0.11f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(
                        width = (right - left).coerceAtLeast(1f),
                        height = plotHeight,
                    ),
                )
            }
        }

        drawLine(
            color = gridColor,
            start = Offset(0f, y(0f)),
            end = Offset(size.width, y(0f)),
            strokeWidth = 1.5.dp.toPx(),
        )
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
        listOf(trace.tailThresholdMg.toFloat(), -trace.tailThresholdMg.toFloat())
            .forEach { threshold ->
                drawLine(
                    color = noiseColor.copy(alpha = 0.8f),
                    start = Offset(0f, y(threshold)),
                    end = Offset(size.width, y(threshold)),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }

        val waveform = Path().apply {
            moveTo(x(0), y(samples.first()))
            for (index in 1 until samples.size) {
                lineTo(x(index), y(samples[index]))
            }
        }
        drawPath(
            path = waveform,
            color = lineColor,
            style = Stroke(width = 1.8.dp.toPx()),
        )

        saturated.forEach { index ->
            if (index in samples.indices) {
                drawCircle(
                    color = rejectionColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x(index), y(samples[index])),
                )
            }
        }

        if (analysisEnd in 1 until samples.size) {
            val endX = x(analysisEnd)
            drawLine(
                color = gridColor,
                start = Offset(endX, top),
                end = Offset(endX, bottom),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dash,
            )
        }

        drawRect(
            color = gridColor,
            topLeft = Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, plotHeight),
            style = Stroke(width = 1.dp.toPx()),
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "0,00 s",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "${decimal(durationSeconds / 2)} s",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "${decimal(durationSeconds)} s",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TraceLegend(trace: MeasurementTraceEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LegendItem(MaterialTheme.colorScheme.primary, "Onda analizada")
        LegendItem(MaterialTheme.colorScheme.surfaceVariant, "Cola de ruido")
        // La región roja y los puntos saturados sólo se dibujan cuando la regla
        // apunta a una zona concreta; anunciarlos en una captura aceptada haría
        // buscar al usuario una marca que no existe.
        if (!trace.valid) {
            if (trace.rejectionScope == "SAMPLES") {
                LegendItem(MaterialTheme.colorScheme.error, "Muestras saturadas")
            } else if (trace.rejectionScope != "HISTORY") {
                LegendItem(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                    "Región de descarte",
                )
            }
        }
        LegendItem(MaterialTheme.colorScheme.tertiary, "Umbral de reposo")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun stageLabel(stage: String): String =
    when (stage) {
        "CAPTURE" -> "captura"
        "SIGNAL_QUALITY" -> "calidad de señal"
        "USABLE_WINDOW" -> "ventana útil"
        "SPECTRUM" -> "análisis de frecuencias"
        "AUTOCORRELATION" -> "periodicidad"
        "CONSISTENCY" -> "coherencia entre métodos"
        "HISTORY" -> "comparación con el historial"
        "ACCEPTED" -> "aceptación"
        else -> stage.lowercase().replace('_', ' ')
    }

private fun scopeLabel(scope: String): String =
    when (scope) {
        "SAMPLES" -> "puntos específicos marcados"
        "RANGE" -> "rango resaltado"
        "HISTORY" -> "decisión respecto al historial"
        else -> "regla aplicada a toda la captura"
    }

private data class DiagnosticCopyResources(
    val summary: Int,
    val action: Int,
    val technical: Int,
)

private fun diagnosticCopyResources(kind: HitDiagnosticKind): DiagnosticCopyResources =
    when (kind) {
        HitDiagnosticKind.SENSOR_SATURATED ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_saturated_summary,
                action = R.string.measure_diagnostic_saturated_action,
                technical = R.string.measure_diagnostic_saturated_technical,
            )

        HitDiagnosticKind.LOW_SIGNAL ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_low_signal_summary,
                action = R.string.measure_diagnostic_low_signal_action,
                technical = R.string.measure_diagnostic_low_signal_technical,
            )

        HitDiagnosticKind.PEAK_OUT_OF_RANGE ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_peak_range_summary,
                action = R.string.measure_diagnostic_peak_range_action,
                technical = R.string.measure_diagnostic_peak_range_technical,
            )

        HitDiagnosticKind.TOO_FEW_CYCLES ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_cycles_summary,
                action = R.string.measure_diagnostic_cycles_action,
                technical = R.string.measure_diagnostic_cycles_technical,
            )

        HitDiagnosticKind.LOW_SNR ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_snr_summary,
                action = R.string.measure_diagnostic_snr_action,
                technical = R.string.measure_diagnostic_snr_technical,
            )

        HitDiagnosticKind.LOW_PERIODICITY ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_periodicity_summary,
                action = R.string.measure_diagnostic_periodicity_action,
                technical = R.string.measure_diagnostic_periodicity_technical,
            )

        HitDiagnosticKind.ESTIMATOR_MISMATCH ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_mismatch_summary,
                action = R.string.measure_diagnostic_mismatch_action,
                technical = R.string.measure_diagnostic_mismatch_technical,
            )

        HitDiagnosticKind.FREQUENCY_SHIFT ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_shift_summary,
                action = R.string.measure_diagnostic_shift_action,
                technical = R.string.measure_diagnostic_shift_technical,
            )

        HitDiagnosticKind.ACCEPTED ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_accepted_summary,
                action = R.string.measure_diagnostic_accepted_action,
                technical = R.string.measure_diagnostic_accepted_technical,
            )

        HitDiagnosticKind.UNKNOWN ->
            DiagnosticCopyResources(
                summary = R.string.measure_diagnostic_unknown_summary,
                action = R.string.measure_diagnostic_unknown_action,
                technical = R.string.measure_diagnostic_unknown_technical,
            )
    }
