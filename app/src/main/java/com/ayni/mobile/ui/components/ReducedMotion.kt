package com.ayni.mobile.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Aproximación estándar en Android para "reduced motion" (§6.4): no hay una API de
 * accesibilidad dedicada pre-API 33 con soporte extendido, así que se usa la escala
 * de duración de animaciones del sistema — 0 significa que el usuario desactivó
 * animaciones en Opciones de desarrollador/Accesibilidad.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }
}
