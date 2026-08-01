package com.ayni.mobile.ui.iot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ayni.mobile.R
import com.ayni.mobile.domain.iot.DiagnosticCount
import com.ayni.mobile.domain.iot.HitDiagnosticKind
import com.ayni.mobile.domain.iot.percentageOf
import com.ayni.mobile.ui.theme.MetricNumberStyle

/**
 * Recuento de capturas por motivo.
 *
 * El objetivo no es decorar: es responder «¿por qué se descartan tantos golpes?»
 * con datos en vez de con intuición. Cada barra es una regla del firmware, y la
 * más larga señala qué umbral revisar.
 */
@Composable
fun DiagnosticTallyCard(
    tally: List<DiagnosticCount>,
    modifier: Modifier = Modifier,
) {
    val total = tally.sumOf { it.count }
    val usable = tally.filterNot { it.rejects }.sumOf { it.count }

    SectionCard(
        title = stringResource(R.string.measure_tally_title),
        subtitle = stringResource(R.string.measure_tally_subtitle),
        modifier = modifier,
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        if (total == 0) {
            Text(
                stringResource(R.string.measure_tally_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }

        Text(
            stringResource(R.string.measure_tally_accepted_share, usable, total),
            style = MaterialTheme.typography.titleMedium,
            color = if (usable * 2 >= total) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        )

        tally.forEach { entry ->
            DiagnosticTallyRow(entry = entry, total = total)
        }

        // Sólo se señala una causa dominante si de verdad domina: por debajo de
        // la mitad de los descartes, apuntar a una sola regla desorienta.
        val dominant = tally.firstOrNull { it.rejects }
        val rejected = tally.filter { it.rejects }.sumOf { it.count }
        if (dominant != null && rejected > 0 && dominant.count * 2 > rejected) {
            Text(
                stringResource(
                    R.string.measure_tally_dominant,
                    stringResource(shortLabelOf(dominant.kind)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticTallyRow(entry: DiagnosticCount, total: Int) {
    val share = percentageOf(entry.count, total)
    val color = when {
        entry.kind == HitDiagnosticKind.FREQUENCY_SHIFT ->
            MaterialTheme.colorScheme.tertiary

        entry.rejects -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(shortLabelOf(entry.kind)),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.measure_tally_row, entry.count, share),
            style = MetricNumberStyle,
            color = color,
        )
    }
    LevelBar(
        value = entry.count.toFloat(),
        maxValue = total.toFloat(),
        color = color,
    )
}

private fun shortLabelOf(kind: HitDiagnosticKind): Int = when (kind) {
    HitDiagnosticKind.SENSOR_SATURATED -> R.string.diagnostic_short_saturated
    HitDiagnosticKind.LOW_SIGNAL -> R.string.diagnostic_short_low_signal
    HitDiagnosticKind.PEAK_OUT_OF_RANGE -> R.string.diagnostic_short_peak_range
    HitDiagnosticKind.TOO_FEW_CYCLES -> R.string.diagnostic_short_cycles
    HitDiagnosticKind.LOW_SNR -> R.string.diagnostic_short_snr
    HitDiagnosticKind.LOW_PERIODICITY -> R.string.diagnostic_short_periodicity
    HitDiagnosticKind.ESTIMATOR_MISMATCH -> R.string.diagnostic_short_mismatch
    HitDiagnosticKind.FREQUENCY_SHIFT -> R.string.diagnostic_short_shift
    HitDiagnosticKind.ACCEPTED -> R.string.diagnostic_short_accepted
    HitDiagnosticKind.UNKNOWN -> R.string.diagnostic_short_unknown
}
