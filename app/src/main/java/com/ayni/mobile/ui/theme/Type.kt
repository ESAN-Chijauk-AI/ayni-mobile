package com.ayni.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO(fonts): el rediseño Stitch pide Inter (narrativa/UI) + JetBrains Mono (datos de
// instrumento). Sin los .ttf en res/font/ todavía, se usan fuentes del sistema como base
// funcional (Roboto se ve razonablemente cercano a Inter; la monoespaciada del sistema
// cumple el mismo rol de "instrumento" que JetBrains Mono). Cambiar estas 2 líneas basta
// para el swap cuando se agreguen los archivos reales.
private val DisplayFont = FontFamily.SansSerif
private val BodyFont = FontFamily.SansSerif
private val MonoFont = FontFamily.Monospace

/**
 * Escala tipográfica 1:1 con stitch_remix_of_ayni_mobile_emergency_response/ayni/DESIGN.md
 * (bloque `typography`). Todo en sp (nunca dp) para respetar el font scale del sistema.
 */
val AyniTypography = Typography(
    // display-lg (34/41, -0.02em, 700) — usos grandes ocasionales (p.ej. veredicto full-bleed).
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.02).sp
    ),
    // display-lg-mobile (28/34, 700) — títulos de página ("Herramientas", "Reporte Estructural")
    // y el label del veredicto en VerdictSemaphore.
    displayMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    // headline-md (24/30, -0.01em, 600) — wordmark "Ayni" en la barra superior.
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.01).sp
    ),
    // headline-sm (20/25, 600) — títulos de tarjeta, nombres de contacto.
    titleLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    // body-lg (17/24, 400) — texto narrativo principal.
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    // body-sm (14/20, 400) — texto secundario/metadata.
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    // data-value (15/20, 600, mono) — lecturas de sensor, GPS, fecha/hora.
    labelMedium = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    // data-label (12/16, 500, +0.05em, mono) — captions en mayúscula tipo instrumento.
    labelSmall = TextStyle(
        fontFamily = MonoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    )
)

/**
 * Mono, para valores numéricos que cambian en vivo (subsistema IoT — MetricRow,
 * DiagnosticTallyCard) — no se desplaza visualmente cuando el valor cambia varias
 * veces por segundo. Ver CLAUDE.md "Subsistema IoT — reglas de no-colisión".
 */
val MetricNumberStyle = TextStyle(
    fontFamily = MonoFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 20.sp
)
