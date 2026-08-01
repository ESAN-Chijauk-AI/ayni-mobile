package com.ayni.mobile.domain.iot

/**
 * Recuento de capturas agrupadas por el motivo que decidió su resultado.
 *
 * Existe para dejar de afinar el algoritmo a ciegas. Cada captura ya guarda su
 * código de diagnóstico; lo que faltaba era contarlos. Si nueve de cada diez
 * golpes se descartan, la distribución dice cuál de las reglas está disparando
 * y por tanto qué parámetro del firmware revisar.
 */
data class DiagnosticCount(
    val kind: HitDiagnosticKind,
    val count: Int,
) {
    val rejects: Boolean get() = kind.rejectsCapture
}

/**
 * @param wireCodes un código por captura. `null` o vacío —una captura sin traza,
 *   por firmware antiguo o notificación perdida— cuenta como [HitDiagnosticKind.UNKNOWN]
 *   en vez de descartarse: un hueco en los datos también es información.
 * @return grupos ordenados de más a menos frecuente. A igual recuento manda el
 *   orden del enum, para que la lista no baile entre recomposiciones.
 */
fun summarizeDiagnostics(wireCodes: List<String?>): List<DiagnosticCount> =
    wireCodes
        .map { HitDiagnosticKind.fromWire(it.orEmpty()) }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<HitDiagnosticKind, Int>> { it.value }
                .thenBy { it.key.ordinal },
        )
        .map { DiagnosticCount(kind = it.key, count = it.value) }

/** Porcentaje entero de [count] sobre [total], para no repetir el redondeo. */
fun percentageOf(count: Int, total: Int): Int =
    if (total <= 0) 0 else (count * 100 + total / 2) / total
