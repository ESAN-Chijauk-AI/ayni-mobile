package com.ayni.mobile.data.ai

/**
 * Prompts exactos de PERQA_AGENT_RULES_GEMMA_SPEED.md §6. Reglas duras:
 * - Sin el token `<|think|>` en ningún system prompt (es lo que activa el thinking mode
 *   y causa latencias de minutos) — NO agregar bajo ninguna circunstancia.
 * - Un prompt por agente, nunca mezclar estructural y médico en un mega-prompt.
 * - Salida SOLO JSON de esquema fijo, sin prosa libre.
 */
object Prompts {

    const val SYSTEM_ESTRUCTURAL = """Rol: inspector estructural de emergencia post-sismo.
Analiza la grieta (imagen) y el dato del acelerometro. Responde SOLO JSON, directo, sin razonar en voz alta:
{"veredicto":"VERDE|AMARILLO|ROJO","razon":"<max 15 palabras>","accion":"<1 frase>"}"""

    const val SYSTEM_MEDICO = """Rol: primer respondiente, triage START (rojo/amarillo/verde/negro).
Dada la lesion, responde SOLO JSON:
{"prioridad":"ROJO|AMARILLO|VERDE","primeros_auxilios":["<paso corto>", ...max 3]}
No des diagnostico definitivo; da accion inmediata con lo que hay a mano."""

    fun estructural(sensorLine: String): String =
        "$SYSTEM_ESTRUCTURAL\nAcelerometro: $sensorLine"

    fun medico(descripcionLesion: String): String =
        "$SYSTEM_MEDICO\nLesion: $descripcionLesion"
}
