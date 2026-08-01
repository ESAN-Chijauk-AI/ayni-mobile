package com.ayni.mobile.data.iot.ble

import java.util.UUID

object BleProtocol {
    const val VERSION = 1

    val SERVICE_UUID: UUID = UUID.fromString("7d2e1000-6b5a-4f6b-9c31-2b6e6d0a2026")
    val STATUS_UUID: UUID = UUID.fromString("7d2e1001-6b5a-4f6b-9c31-2b6e6d0a2026")
    val COMMAND_UUID: UUID = UUID.fromString("7d2e1002-6b5a-4f6b-9c31-2b6e6d0a2026")
    val EVENT_UUID: UUID = UUID.fromString("7d2e1003-6b5a-4f6b-9c31-2b6e6d0a2026")
    val STRUCTURAL_STATE_UUID: UUID =
        UUID.fromString("7d2e1004-6b5a-4f6b-9c31-2b6e6d0a2026")
    val PROTOCOL_UUID: UUID = UUID.fromString("7d2e1005-6b5a-4f6b-9c31-2b6e6d0a2026")
    val TRACE_UUID: UUID = UUID.fromString("7d2e1006-6b5a-4f6b-9c31-2b6e6d0a2026")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Longitud máxima de la característica de comando en el firmware. */
    const val COMMAND_MAX_BYTES = 128

    fun command(commandId: Long, name: String, argument: Long? = null): ByteArray =
        command(commandId, name, argument?.toString())

    /**
     * El nodo parte el envelope por los tres primeros separadores y conserva el
     * resto tal cual, así que un argumento de texto puede contener `|`.
     */
    fun command(commandId: Long, name: String, argument: String?): ByteArray =
        buildString {
            append(VERSION)
            append('|')
            append(commandId)
            append('|')
            append(name)
            if (argument != null) {
                append('|')
                append(argument)
            }
        }.encodeToByteArray()
}
