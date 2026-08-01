package com.ayni.mobile.data.proximity

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProximityProtocolTest {
    @Test
    fun `capabilities preserve optional UWB address`() {
        val address = byteArrayOf(0x12, 0x34)
        val decoded = ProximityProtocol.decodeCapabilities(
            ProximityProtocol.encodeCapabilities(address),
        )
        assertNotNull(decoded)
        assertArrayEquals(address, decoded?.uwbAddress)
    }

    @Test
    fun `handshake fits default GATT payload and round trips`() {
        val source = UwbHandshake(
            sessionId = 42,
            sessionKey = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
            controllerAddress = byteArrayOf(9, 10),
            channel = 9,
            preambleIndex = 11,
        )
        val encoded = ProximityProtocol.encodeUwbHandshake(source)
        val decoded = ProximityProtocol.decodeUwbHandshake(encoded)

        assertEquals(19, encoded.size)
        assertNotNull(decoded)
        assertEquals(source.sessionId, decoded?.sessionId)
        assertArrayEquals(source.sessionKey, decoded?.sessionKey)
        assertArrayEquals(source.controllerAddress, decoded?.controllerAddress)
        assertEquals(source.channel, decoded?.channel)
        assertEquals(source.preambleIndex, decoded?.preambleIndex)
    }

    @Test
    fun `rejects malformed handshake`() {
        assertNull(ProximityProtocol.decodeUwbHandshake(byteArrayOf(1, 2, 3)))
    }
}
