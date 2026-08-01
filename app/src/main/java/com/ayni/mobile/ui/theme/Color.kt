package com.ayni.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Sistema "Honey Amber" (rediseño Stitch, ../stitch_remix_of_ayni_mobile_emergency_response).
// Reemplaza el theme dark-first original: "calma con autoridad", cálido, tipo instrumento
// de precisión. Fondo cálido tipo papel + tarjetas blancas elevadas.
val AyniSurface = Color(0xFFFFF8F0)
val AyniSurfaceDim = Color(0xFFE0D9CE)
val AyniSurfaceBright = Color(0xFFFFF8F0)
val AyniSurfaceContainerLowest = Color(0xFFFFFFFF)
val AyniSurfaceContainerLow = Color(0xFFFAF3E7)
val AyniSurfaceContainer = Color(0xFFF5EDE1)
val AyniSurfaceContainerHigh = Color(0xFFEFE7DC)
val AyniSurfaceContainerHighest = Color(0xFFE9E1D6)

val AyniOnSurface = Color(0xFF1E1B14)
val AyniOnSurfaceVariant = Color(0xFF504535)
val AyniOutline = Color(0xFF827563)
val AyniOutlineVariant = Color(0xFFD4C4AF)
val AyniHairline = Color(0xFFECE7DC)

val AyniPrimary = Color(0xFF7D5800)
val AyniOnPrimary = Color(0xFFFFFFFF)
val AyniPrimaryContainer = Color(0xFFF4B740) // acento ámbar principal (CTAs, iconos, "instrumento")
val AyniOnPrimaryContainer = Color(0xFF694900)
val AyniPrimaryFixed = Color(0xFFFFDEA9) // fondos suaves (círculos de ícono, chips)
val AyniPrimaryFixedDim = Color(0xFFFABC45)
val AyniBrandSoft = Color(0xFFFCE9BF) // anillo exterior del botón SOS

val AyniSecondary = Color(0xFF645E4E)
val AyniSecondaryContainer = Color(0xFFEBE2CD)
val AyniInputBackground = Color(0xFFFDF3DE)

val AyniTertiary = Color(0xFF006D3E)
val AyniTertiaryContainer = Color(0xFF67D794)

val AyniError = Color(0xFFBA1A1A) // badges/alertas generales
val AyniOnError = Color(0xFFFFFFFF)
val AyniErrorContainer = Color(0xFFFFDAD6)
val AyniOnErrorContainer = Color(0xFF93000A)
val AyniDangerRed = Color(0xFFDC5B4B) // núcleo del botón SOS específicamente (marca, no M3 error)

// Semántica de triage (ATC-20 / START). NO cambiar por gusto: es información, no decoración
// (spec §6.2) — intocable pese al rediseño visual completo del resto del theme.
val Verde = Color(0xFF2FBF71)
val Amarillo = Color(0xFFF5B400)
val Rojo = Color(0xFFE5484D)
val Negro = Color(0xFF3A3F46)
val NegroLabel = Color(0xFFE8EAED)

/**
 * Acento "instrumento" reutilizado en el readout de sensor y en íconos de marca.
 * Antes era cian (#4CC9F0) en el theme dark-first original; con el rediseño Stitch pasa
 * a ámbar (mismo primary-container) para que "Pulso Estructural" y los demás usos
 * combinen con el nuevo sistema — el rol semántico (acento quieto, se calla ante el
 * semáforo) no cambia, solo el hue.
 */
val Signal = AyniPrimaryContainer

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

// Aliases retrocompatibles con nombres del theme dark-first anterior, para no tener que
// tocar cada archivo que ya importaba Surface/SurfaceRaised/Outline/OnSurface/OnSurfaceMuted.
val Surface = AyniSurface
val SurfaceRaised = AyniSurfaceContainerLowest
val Outline = AyniOutlineVariant
val OnSurface = AyniOnSurface
val OnSurfaceMuted = AyniOnSurfaceVariant
