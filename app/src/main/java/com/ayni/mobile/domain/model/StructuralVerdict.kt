package com.ayni.mobile.domain.model

/**
 * Semáforo de triage estructural, estilo ATC-20. El orden VERDE < AMARILLO < ROJO
 * importa: el sensor solo puede subir el veredicto, nunca bajarlo (§7 del spec: ante duda, sube).
 */
enum class StructuralVerdict { VERDE, AMARILLO, ROJO }
