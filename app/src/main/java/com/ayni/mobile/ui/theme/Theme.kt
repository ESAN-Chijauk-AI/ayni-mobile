package com.ayni.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Dark-first por diseño (§6.1: penumbra post-desastre, batería OLED, menos deslumbramiento).
 * El spec no pide light mode para esta app — se usa un único darkColorScheme fijo,
 * independiente de la preferencia del sistema (isSystemInDarkTheme no se consulta a
 * propósito: la app siempre se ve igual, sin sorpresas).
 */
private val AyniColorScheme = darkColorScheme(
    primary = Signal,
    onPrimary = Surface,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = OnSurfaceMuted,
    outline = Outline,
    error = Rojo,
    onError = OnSurface
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
