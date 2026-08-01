package com.ayni.mobile.domain.proximity

import org.junit.Assert.assertEquals
import org.junit.Test

class RssiSignalTrackerTest {

    @Test
    fun `clasifica intensidad sin convertirla a distancia`() {
        assertEquals(
            ProximitySignalLevel.WEAK,
            RssiSignalTracker().add(rssi = -90, elapsedMs = 0).level,
        )
        assertEquals(
            ProximitySignalLevel.MEDIUM,
            RssiSignalTracker().add(rssi = -75, elapsedMs = 0).level,
        )
        assertEquals(
            ProximitySignalLevel.STRONG,
            RssiSignalTracker().add(rssi = -60, elapsedMs = 0).level,
        )
    }

    @Test
    fun `detecta acercamiento solo despues de una ventana suficiente`() {
        val tracker = RssiSignalTracker()
        assertEquals(ProximityTrend.UNKNOWN, tracker.add(-90, 0).trend)
        assertEquals(ProximityTrend.UNKNOWN, tracker.add(-88, 1_000).trend)
        assertEquals(ProximityTrend.APPROACHING, tracker.add(-58, 2_600).trend)
    }

    @Test
    fun `detecta alejamiento por cambio sostenido de intensidad`() {
        val tracker = RssiSignalTracker()
        tracker.add(-55, 0)
        tracker.add(-58, 1_000)
        assertEquals(ProximityTrend.MOVING_AWAY, tracker.add(-90, 2_600).trend)
    }

    @Test
    fun `mantiene estable cuando la variacion queda bajo la histeresis`() {
        val tracker = RssiSignalTracker()
        tracker.add(-72, 0)
        tracker.add(-71, 1_200)
        assertEquals(ProximityTrend.STABLE, tracker.add(-70, 2_600).trend)
    }
}
