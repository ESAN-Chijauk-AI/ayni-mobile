package com.ayni.mobile.domain.usecase

import com.ayni.mobile.domain.model.StructuralHitReading
import com.ayni.mobile.domain.model.StructuralSafetyAnalysis
import com.ayni.mobile.domain.model.StructuralVerdict
import com.ayni.mobile.domain.repository.AiRepository
import javax.inject.Inject

class AnalyzeStructuralHitsUseCase @Inject constructor(
    private val aiRepository: AiRepository,
) {
    suspend operator fun invoke(hits: List<StructuralHitReading>): StructuralSafetyAnalysis {
        require(hits.size in MINIMUM_HIT_COUNT..MAXIMUM_HIT_COUNT) {
            "Se requieren entre $MINIMUM_HIT_COUNT y $MAXIMUM_HIT_COUNT golpes válidos"
        }

        val oldestToNewest = hits.sortedBy(StructuralHitReading::measuredAtEpochMs)
        val modelResult = aiRepository.analyzeStructuralHits(oldestToNewest)
        val frequencyShift = oldestToNewest.any {
            it.reason.contains("FREQUENCY_SHIFT", ignoreCase = true)
        }
        val unavailable = modelResult.razon.contains(
            "No se pudo evaluar automáticamente",
            ignoreCase = true,
        )

        return civilStructuralGuidance(
            verdict = modelResult.verdict,
            frequencyShift = frequencyShift,
            analysisUnavailable = unavailable,
        )
    }

    companion object {
        const val MINIMUM_HIT_COUNT = 3
        const val MAXIMUM_HIT_COUNT = 10
    }
}

internal fun civilStructuralGuidance(
    verdict: StructuralVerdict,
    frequencyShift: Boolean,
    analysisUnavailable: Boolean,
): StructuralSafetyAnalysis = when {
    analysisUnavailable -> StructuralSafetyAnalysis(
        verdict = StructuralVerdict.AMARILLO,
        summary = "No se puede decidir si conviene permanecer. Actúa con precaución.",
        steps = listOf(
            "Evacúa si ves grietas nuevas, inclinación, humo, agua o cosas inestables.",
            "Busca un lugar abierto y solicita una revisión antes de volver a entrar.",
        ),
    )

    verdict == StructuralVerdict.ROJO -> StructuralSafetyAnalysis(
        verdict = StructuralVerdict.ROJO,
        summary = "Evacúa el espacio inmediatamente. Gemma encontró cambios que pueden indicar peligro.",
        steps = listOf(
            "Sal ahora por la ruta más corta que esté libre de peligros.",
            "Ayuda a niños y otras personas sin detenerte a recoger objetos.",
            "Aléjate de paredes, ventanas, postes y cosas que puedan caer.",
        ),
    )

    frequencyShift || verdict == StructuralVerdict.AMARILLO -> StructuralSafetyAnalysis(
        verdict = StructuralVerdict.AMARILLO,
        summary = "Se sugiere evacuar el espacio con calma. Gemma encontró cambios que deben revisarse.",
        steps = listOf(
            "Sal con calma por una ruta despejada y ayuda a quien lo necesite.",
            "No uses ascensores ni pases cerca de paredes, vidrios u objetos inestables.",
            "Permanece afuera hasta que una persona capacitada revise el lugar.",
        ),
    )

    else -> StructuralSafetyAnalysis(
        verdict = StructuralVerdict.VERDE,
        summary = "Puedes permanecer dentro por ahora. Los golpes recientes son parecidos.",
        steps = listOf(
            "Mira si hay grietas nuevas, partes inclinadas o cosas que puedan caer.",
            "Mantén a niños y otras personas lejos de zonas dañadas.",
            "Evacúa si ves daños, escuchas crujidos o notas nuevos movimientos.",
        ),
    )
}
