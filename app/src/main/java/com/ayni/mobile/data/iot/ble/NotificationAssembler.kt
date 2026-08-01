package com.ayni.mobile.data.iot.ble

import java.util.UUID

class NotificationAssembler(
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private data class Key(
        val characteristic: UUID,
        val messageId: Int,
    )

    private data class PendingMessage(
        val chunkCount: Int,
        val chunks: Array<ByteArray?>,
        var updatedAtMs: Long,
    )

    private val pending = mutableMapOf<Key, PendingMessage>()

    @Synchronized
    fun accept(characteristic: UUID, packet: ByteArray): String? {
        require(packet.size >= HEADER_SIZE) { "Notificación BLE sin cabecera completa" }
        val version = packet[0].toInt() and 0xff
        val messageId = packet[1].toInt() and 0xff
        val chunkIndex = packet[2].toInt() and 0xff
        val chunkCount = packet[3].toInt() and 0xff

        require(version == BleProtocol.VERSION) { "Versión BLE no soportada: $version" }
        require(messageId != 0) { "messageId inválido" }
        require(chunkCount in 1..MAX_CHUNKS) { "Cantidad de fragmentos inválida" }
        require(chunkIndex < chunkCount) { "Índice de fragmento inválido" }

        pruneExpired()
        val key = Key(characteristic, messageId)
        val message = pending[key]?.takeIf { it.chunkCount == chunkCount }
            ?: PendingMessage(
                chunkCount = chunkCount,
                chunks = arrayOfNulls(chunkCount),
                updatedAtMs = nowMs(),
            ).also { pending[key] = it }

        message.chunks[chunkIndex] = packet.copyOfRange(HEADER_SIZE, packet.size)
        message.updatedAtMs = nowMs()
        if (message.chunks.any { it == null }) return null

        val size = message.chunks.sumOf { it!!.size }
        val complete = ByteArray(size)
        var offset = 0
        message.chunks.forEach { chunk ->
            chunk!!
            chunk.copyInto(complete, offset)
            offset += chunk.size
        }
        pending.remove(key)
        return complete.toString(Charsets.UTF_8)
    }

    @Synchronized
    fun clear() {
        pending.clear()
    }

    private fun pruneExpired() {
        val threshold = nowMs() - EXPIRATION_MS
        pending.entries.removeAll { it.value.updatedAtMs < threshold }
    }

    private companion object {
        const val HEADER_SIZE = 4
        const val MAX_CHUNKS = 255
        const val EXPIRATION_MS = 10_000L
    }
}
