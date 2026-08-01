package com.ayni.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Sistema "Honey Amber" (rediseño Stitch): cálido, tipo "instrumento de precisión" con
 * calma-autoridad, fondo papel cálido + tarjetas blancas elevadas. Reemplaza el
 * dark-first original — un único lightColorScheme fijo, sin alternar con el sistema
 * (misma lógica de "la app siempre se ve igual, sin sorpresas" que antes).
 */
private val AyniColorScheme = lightColorScheme(
    primary = AyniPrimary,
    onPrimary = AyniOnPrimary,
    primaryContainer = AyniPrimaryContainer,
    onPrimaryContainer = AyniOnPrimaryContainer,
    secondary = AyniSecondary,
    secondaryContainer = AyniSecondaryContainer,
    // OJO: tertiary = Amarillo (NO el verde de Stitch) — reservado por el subsistema IoT
    // como color del invariante FREQUENCY_SHIFT (CLAUDE.md, "reglas de no-colisión").
    // El verde de marca de Stitch (#006D3E, AyniTertiary) sigue disponible como color
    // suelto para quien lo necesite directo, solo no vive en este slot de colorScheme.
    tertiary = Amarillo,
    tertiaryContainer = AyniPrimaryFixed,
    background = AyniSurface,
    onBackground = AyniOnSurface,
    surface = AyniSurface,
    onSurface = AyniOnSurface,
    surfaceVariant = AyniSurfaceContainerHigh,
    onSurfaceVariant = AyniOnSurfaceVariant,
    surfaceContainerLowest = AyniSurfaceContainerLowest,
    surfaceContainerLow = AyniSurfaceContainerLow,
    surfaceContainer = AyniSurfaceContainer,
    surfaceContainerHigh = AyniSurfaceContainerHigh,
    surfaceContainerHighest = AyniSurfaceContainerHighest,
    outline = AyniOutline,
    outlineVariant = AyniOutlineVariant,
    error = AyniError,
    onError = AyniOnError,
    errorContainer = AyniErrorContainer,
    onErrorContainer = AyniOnErrorContainer
)

@Composable
fun AyniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AyniColorScheme,
        typography = AyniTypography,
        shapes = AyniShapes,
        content = content
    )
}
