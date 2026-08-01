package com.ayni.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** "Hyper-Rounded" — rediseño Stitch: tarjetas 24dp, botones/inputs 16dp, full para SOS/estados. */
val AyniShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** Grid de espaciado 1:1 con DESIGN.md (`spacing`). */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val stackGap = 12.dp
    val md = 16.dp // gutter-card
    val marginPage = 20.dp
    val lg = 24.dp // container-padding
    val xl = 32.dp // section-gap
    val xxl = 48.dp
}

/** Tap target mínimo para acciones primarias (§6.1 del spec original: "mínimo 56dp de alto"). */
val MinTapTarget = 56.dp
