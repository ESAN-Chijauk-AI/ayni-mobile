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
    // secondary/tertiary los consume el subsistema IoT (tarjetas de monitoreo). tertiary =
    // Amarillo respeta el invariante ATC-20/estructural: un FREQUENCY_SHIFT es ÁMBAR, no rojo
    // (ver ProtoEstados AGENT.md, invariante 6). No toca los colores de veredicto (Verde/Amarillo/Rojo).
    secondary = Signal,
    onSecondary = Surface,
    tertiary = Amarillo,
    onTertiary = Surface,
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
