package com.ayni.mobile.data.local

import com.ayni.mobile.domain.model.StructuralResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class CompletedStructuralReport(
    val result: StructuralResult,
    val imageBytes: ByteArray?,
    val timestampMs: Long
)

/**
 * Último reporte estructural completado en esta sesión de proceso, en memoria. Permite
 * que la pestaña "Reportes" del bottom nav (rediseño Stitch) muestre el último análisis
 * sin depender de haber llegado ahí a través del flujo de captura — StructuralViewModel
 * escribe aquí al terminar un análisis exitoso, ReportsScreen solo lee.
 *
 * No es F7 (historial persistente) — eso sigue fuera de alcance; esto se pierde al matar
 * el proceso, a propósito (mismo criterio que DisclaimerAcknowledgedState).
 */
@Singleton
class LastStructuralReportState @Inject constructor() {
    private val _lastReport = MutableStateFlow<CompletedStructuralReport?>(null)
    val lastReport: StateFlow<CompletedStructuralReport?> = _lastReport

    fun update(result: StructuralResult, imageBytes: ByteArray?) {
        _lastReport.value = CompletedStructuralReport(result, imageBytes, System.currentTimeMillis())
    }
}
