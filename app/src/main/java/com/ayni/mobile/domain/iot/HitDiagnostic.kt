package com.ayni.mobile.domain.iot

/**
 * Resultado semántico del diagnóstico de una captura.
 *
 * El protocolo conserva códigos compactos y estables; la interfaz trabaja con
 * este tipo para no dispersar comparaciones de cadenas ni confundir
 * `FREQUENCY_SHIFT` —válido— con un descarte.
 */
enum class HitDiagnosticKind(
    val wireCode: String,
    val rejectsCapture: Boolean,
) {
    SENSOR_SATURATED("SENSOR_SATURATED", true),
    LOW_SIGNAL("LOW_SIGNAL", true),
    PEAK_OUT_OF_RANGE("PEAK_OUT_OF_RANGE", true),
    TOO_FEW_CYCLES("TOO_FEW_CYCLES", true),
    LOW_SNR("LOW_SNR", true),
    LOW_PERIODICITY("LOW_PERIODICITY", true),
    ESTIMATOR_MISMATCH("ESTIMATOR_MISMATCH", true),
    FREQUENCY_SHIFT("FREQUENCY_SHIFT", false),
    ACCEPTED("ACCEPTED", false),
    UNKNOWN("", false),
    ;

    companion object {
        fun fromWire(value: String): HitDiagnosticKind {
            val normalized = value.trim()
            return entries.firstOrNull {
                it != UNKNOWN && it.wireCode.equals(normalized, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}
