package com.ayni.mobile.domain.iot

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Formato compartido por el ViewModel y la interfaz. Vive en `domain` para que
 * un UUID o una frecuencia se lean igual en una tarjeta, en un mensaje de error
 * y en el inspector de Room.
 */

fun shortDeviceId(deviceId: String): String =
    if (deviceId.length <= 12) deviceId
    else "${deviceId.take(8)}…${deviceId.takeLast(4)}"

fun decimal(value: Double, digits: Int = 2): String =
    String.format(Locale.getDefault(), "%.${digits}f", value)

fun hz(value: Double): String = "${decimal(value)} Hz"

fun angle(value: Double): String = "${decimal(value)}°"

fun signed(value: Double): String =
    String.format(Locale.getDefault(), "%+.2f", value)

fun dateTime(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(epochMs))

/** Diferencia angular envuelta a (-180, 180]; el roll cruza ±180°. */
fun angleDelta(before: Double, after: Double): Double {
    var delta = (after - before) % 360.0
    if (delta > 180.0) delta -= 360.0
    if (delta < -180.0) delta += 360.0
    return delta
}
