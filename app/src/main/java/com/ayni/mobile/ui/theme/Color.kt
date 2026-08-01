package com.ayni.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Superficies (dark-first, no negro puro para evitar smearing OLED). Spec §6.2 — no cambiar.
val Surface = Color(0xFF0E1116)
val SurfaceRaised = Color(0xFF161B22)
val Outline = Color(0xFF2A3038)

// Texto
val OnSurface = Color(0xFFE8EAED)
val OnSurfaceMuted = Color(0xFF9AA4B2)

// Semántica de triage (ATC-20 / START). NO cambiar por gusto: es información, no decoración.
val Verde = Color(0xFF2FBF71)
val Amarillo = Color(0xFFF5B400)
val Rojo = Color(0xFFE5484D)
val Negro = Color(0xFF3A3F46)
val NegroLabel = Color(0xFFE8EAED) // label claro sobre el fondo "negro" de START

// Acento de marca (quieto; se calla en la pantalla de resultado, donde manda el semántico).
val Signal = Color(0xFF4CC9F0)

/**
 * Colores semánticos de veredicto, deliberadamente FUERA del ColorScheme de Material3:
 * mapearlos a slots estándar (primary/error/etc.) forzaría una semántica de Material
 * ajena al significado de triage, que el spec marca como intocable. Se acceden vía
 * AyniTheme.semanticColors (ver Theme.kt) o directamente importando este objeto.
 */
object AyniSemanticColors {
    val verde = Verde
    val amarillo = Amarillo
    val rojo = Rojo
    val negro = Negro
    val negroLabel = NegroLabel
    val signal = Signal
}
