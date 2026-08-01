package com.ayni.mobile.data.proximity

import java.util.UUID
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ProximityProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("7c7b5d20-74f1-4d76-a8c9-41594e594e49")
    val CAPABILITIES_UUID: UUID = UUID.fromString("7c7b5d21-74f1-4d76-a8c9-41594e594e49")
    val CONTROL_UUID: UUID = UUID.fromString("7c7b5d22-74f1-4d76-a8c9-41594e594e49")

    const val VERSION: Byte = 1
    const val COMMAND_CONFIRM: Byte = 1
    const val COMMAND_START_UWB: Byte = 2
    private const val FLAG_UWB = 1

    fun encodeCapabilities(uwbAddress: ByteArray?): ByteArray {
        val address = uwbAddress?.takeIf { it.size <= 8 } ?: byteArrayOf()
        return byteArrayOf(VERSION, if (address.isNotEmpty()) FLAG_UWB.toByte() else 0, address.size.toByte()) + address
    }

    fun decodeCapabilities(value: ByteArray): ProximityCapabilities? {
        if (value.size < 3 || value[0] != VERSION) return null
        val addressLength = value[2].toInt() and 0xFF
        if (addressLength !in 0..8 || value.size < 3 + addressLength) return null
        return ProximityCapabilities(
            uwbAddress = if (value[1].toInt() and FLAG_UWB != 0 && addressLength > 0) {
                value.copyOfRange(3, 3 + addressLength)
            } else {
                null
            },
        )
    }

    fun encodeUwbHandshake(handshake: UwbHandshake): ByteArray = ByteBuffer
        .allocate(19 + (handshake.controllerAddress.size - 2))
        .order(ByteOrder.BIG_ENDIAN)
        .put(VERSION)
        .put(COMMAND_START_UWB)
        .putInt(handshake.sessionId)
        .put(handshake.sessionKey.copyOf(8))
        .put(handshake.controllerAddress.size.toByte())
        .put(handshake.controllerAddress)
        .put(handshake.channel.toByte())
        .put(handshake.preambleIndex.toByte())
        .array()

    fun decodeUwbHandshake(value: ByteArray): UwbHandshake? {
        if (value.size < 17 || value[0] != VERSION || value[1] != COMMAND_START_UWB) return null
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
        buffer.position(2)
        val sessionId = buffer.int
        val key = ByteArray(8).also(buffer::get)
        val addressLength = buffer.get().toInt() and 0xFF
        if (addressLength !in 2..8 || buffer.remaining() < addressLength + 2) return null
        val address = ByteArray(addressLength).also(buffer::get)
        return UwbHandshake(
            sessionId = sessionId,
            sessionKey = key,
            controllerAddress = address,
            channel = buffer.get().toInt() and 0xFF,
            preambleIndex = buffer.get().toInt() and 0xFF,
        )
    }
}

data class ProximityCapabilities(val uwbAddress: ByteArray?)

data class UwbHandshake(
    val sessionId: Int,
    val sessionKey: ByteArray,
    val controllerAddress: ByteArray,
    val channel: Int,
    val preambleIndex: Int,
)
