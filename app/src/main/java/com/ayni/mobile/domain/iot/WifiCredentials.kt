package com.ayni.mobile.domain.iot

/**
 * Reglas de la red que el nodo acepta.
 *
 * Son espejo de las que aplica el firmware en `guardarCredencialesWifi`. Se
 * validan aquí para no gastar un viaje BLE en un comando que va a ser
 * rechazado, y sobre todo para poder explicar el motivo antes de enviarlo.
 */
enum class WifiCredentialsProblem {
    SSID_REQUIRED,
    SSID_TOO_LONG,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    DOES_NOT_FIT_IN_COMMAND,
}

private const val SSID_MAX_BYTES = 32
private const val PASSWORD_MIN_BYTES = 8
private const val PASSWORD_MAX_BYTES = 63

/**
 * La característica de comando admite 128 bytes. El envelope
 * `1|<commandId>|SET_WIFI|` gasta hasta 22 y el separador entre red y clave
 * otro, así que quedan 105 para el par.
 */
private const val COMMAND_PAYLOAD_MAX_BYTES = 105

/** `null` si la red es válida; el motivo del rechazo en caso contrario. */
fun validateWifiCredentials(ssid: String, password: String): WifiCredentialsProblem? {
    // Se mide en bytes y no en caracteres: el firmware valida con `strlen`, y un
    // SSID con acentos o emoji ocupa más de lo que aparenta en pantalla.
    val ssidBytes = ssid.toByteArray(Charsets.UTF_8).size
    val passwordBytes = password.toByteArray(Charsets.UTF_8).size
    return when {
        ssidBytes == 0 -> WifiCredentialsProblem.SSID_REQUIRED
        ssidBytes > SSID_MAX_BYTES -> WifiCredentialsProblem.SSID_TOO_LONG
        // Vacía es válida: red abierta. Entre 1 y 7 no lo es, porque WPA2 exige
        // ocho y el nodo quedaría sin poder asociarse.
        passwordBytes in 1 until PASSWORD_MIN_BYTES ->
            WifiCredentialsProblem.PASSWORD_TOO_SHORT

        passwordBytes > PASSWORD_MAX_BYTES -> WifiCredentialsProblem.PASSWORD_TOO_LONG
        ssidBytes + 1 + passwordBytes > COMMAND_PAYLOAD_MAX_BYTES ->
            WifiCredentialsProblem.DOES_NOT_FIT_IN_COMMAND

        else -> null
    }
}

/** El nodo parte por el primer separador, así que la clave puede contener `|`. */
fun wifiCommandArgument(ssid: String, password: String): String = "$ssid|$password"
