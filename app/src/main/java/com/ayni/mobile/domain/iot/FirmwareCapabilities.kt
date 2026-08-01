package com.ayni.mobile.domain.iot

/**
 * Qué puede hacer el nodo según la versión que publica en el estado operativo.
 *
 * La aplicación admite firmware más antiguo que ella, así que una función nueva
 * no puede asumir que el nodo la implementa. Antes que fallar en silencio, la
 * interfaz explica por qué algo no se mueve.
 */

/** Primera versión que notifica el snapshot estructural de forma periódica. */
private const val LIVE_REST_TELEMETRY_MIN = "0.7.0"

/**
 * Con firmware anterior, el snapshot sólo se notifica al volver a «listo», tras
 * un golpe o ante `GET_STATE`. En reposo las barras de comprobación del sensor
 * se quedarían quietas y parecerían un fallo del hardware.
 */
fun supportsLiveRestTelemetry(firmwareVersion: String): Boolean =
    compareFirmwareVersions(firmwareVersion, LIVE_REST_TELEMETRY_MIN) >= 0

/**
 * Compara `mayor.menor.parche`. Una versión vacía o ilegible se trata como la
 * más antigua: ante la duda se muestra la advertencia, que es el fallo seguro.
 */
internal fun compareFirmwareVersions(first: String, second: String): Int {
    val a = versionParts(first)
    val b = versionParts(second)
    for (index in 0 until maxOf(a.size, b.size)) {
        val left = a.getOrElse(index) { 0 }
        val right = b.getOrElse(index) { 0 }
        if (left != right) return left.compareTo(right)
    }
    return 0
}

private fun versionParts(value: String): List<Int> =
    value.trim()
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
