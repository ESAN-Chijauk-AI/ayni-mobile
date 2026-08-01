package com.ayni.mobile.domain.model

/**
 * Prioridad de triage START. NEGRO requiere manejo especial de copy/UI (§7 del spec):
 * nunca mostrarlo frío o deshumanizado, siempre dirigir a ayuda profesional.
 */
enum class MedicalPriority { ROJO, AMARILLO, VERDE, NEGRO }
