package com.ayni.mobile.data.ai

import com.ayni.mobile.domain.model.MedicalPriority
import com.ayni.mobile.domain.model.MedicalResult
import com.ayni.mobile.domain.model.StructuralResult
import com.ayni.mobile.domain.model.StructuralVerdict
import kotlinx.serialization.json.Json

/**
 * Parseo tolerante del JSON de Gemma + fallback seguro (spec §4 y §6 de speed rules):
 * "JSON malformado → un reintento; si falla, fallback seguro (estructural: AMARILLO
 * 'evaluación manual'; médico: AMARILLO 'buscar ayuda')". El reintento vive en
 * GemmaAiRepository, no aquí — esta clase solo intenta parsear una respuesta y
 * expone los valores de fallback para que el repositorio los use tras agotar reintentos.
 */
object TriageJsonParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseStructural(raw: String): StructuralResult? = runCatching {
        val dto = json.decodeFromString(StructuralJson.serializer(), extractJsonObject(raw))
        StructuralResult(
            verdict = StructuralVerdict.valueOf(dto.veredicto.trim().uppercase()),
            razon = dto.razon,
            accion = dto.accion,
            usoSensor = false // el caller (GemmaAiRepository) lo sobrescribe con .copy()
        )
    }.getOrNull()

    fun parseMedical(raw: String): MedicalResult? = runCatching {
        val dto = json.decodeFromString(MedicalJson.serializer(), extractJsonObject(raw))
        MedicalResult(
            prioridad = MedicalPriority.valueOf(dto.prioridad.trim().uppercase()),
            primerosAuxilios = dto.primeros_auxilios.take(3)
        )
    }.getOrNull()

    /**
     * Gemma a veces envuelve el JSON en texto/markdown pese a la instrucción de "SOLO JSON".
     * Recorta al primer '{' .. último '}' para tolerar eso sin gastar tokens extra en el prompt.
     */
    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
    }

    val STRUCTURAL_FALLBACK = StructuralResult(
        verdict = StructuralVerdict.AMARILLO,
        razon = "No se pudo evaluar automáticamente",
        accion = "Evaluación manual recomendada",
        usoSensor = false
    )

    val MEDICAL_FALLBACK = MedicalResult(
        prioridad = MedicalPriority.AMARILLO,
        primerosAuxilios = listOf("Busca ayuda profesional")
    )
}
