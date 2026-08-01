package com.ayni.mobile.domain.proximity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityPulseCueTest {
    @Test
    fun `a menos 50 dBm produce beeps rapidos y agudos`() {
        val cue = proximityPulseCue(smoothedRssi = -50.0, distanceMeters = null)

        assertTrue(cue.intervalMillis <= 180L)
        assertTrue(cue.toneFrequencyHz >= 1_900)
    }

    @Test
    fun `cadencia y tono aumentan de forma monotona al acercarse`() {
        val far = proximityPulseCue(-92.0, null)
        val medium = proximityPulseCue(-75.0, null)
        val near = proximityPulseCue(-52.0, null)

        assertTrue(far.intervalMillis > medium.intervalMillis)
        assertTrue(medium.intervalMillis > near.intervalMillis)
        assertTrue(far.toneFrequencyHz < medium.toneFrequencyHz)
        assertTrue(medium.toneFrequencyHz < near.toneFrequencyHz)
    }

    @Test
    fun `UWB tiene prioridad cuando entrega distancia`() {
        val nearByUwb = proximityPulseCue(smoothedRssi = -92.0, distanceMeters = 0.5f)
        val expected = proximityPulseCue(smoothedRssi = null, distanceMeters = 0.5f)

        assertEquals(expected, nearByUwb)
    }
}
