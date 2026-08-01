package com.ayni.mobile.domain.usecase

import com.ayni.mobile.domain.model.StructuralVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuralSafetyGuidanceTest {
    @Test
    fun `green result permits a conditional stay`() {
        val analysis = civilStructuralGuidance(
            verdict = StructuralVerdict.VERDE,
            frequencyShift = false,
            analysisUnavailable = false,
        )

        assertEquals(StructuralVerdict.VERDE, analysis.verdict)
        assertTrue(analysis.summary.startsWith("Puedes permanecer dentro por ahora"))
        assertTrue(analysis.steps.size <= 3)
    }

    @Test
    fun `frequency shift never permits a stay`() {
        val analysis = civilStructuralGuidance(
            verdict = StructuralVerdict.VERDE,
            frequencyShift = true,
            analysisUnavailable = false,
        )

        assertEquals(StructuralVerdict.AMARILLO, analysis.verdict)
        assertTrue(analysis.summary.startsWith("Se sugiere evacuar"))
    }

    @Test
    fun `red result orders immediate evacuation`() {
        val analysis = civilStructuralGuidance(
            verdict = StructuralVerdict.ROJO,
            frequencyShift = false,
            analysisUnavailable = false,
        )

        assertEquals(StructuralVerdict.ROJO, analysis.verdict)
        assertTrue(analysis.summary.startsWith("Evacúa el espacio inmediatamente"))
    }

    @Test
    fun `unavailable model never presents a confident result`() {
        val analysis = civilStructuralGuidance(
            verdict = StructuralVerdict.VERDE,
            frequencyShift = false,
            analysisUnavailable = true,
        )

        assertEquals(StructuralVerdict.AMARILLO, analysis.verdict)
        assertTrue(analysis.summary.startsWith("No se puede decidir"))
    }
}
