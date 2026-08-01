package com.ayni.mobile.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Patrones hápticos distintos por severidad (§6.1 y §6.4: "cada estado del semáforo
 * lleva también... patrón háptico, para daltonismo"). NEGRO usa el patrón más suave
 * a propósito: el spec pide manejar esa prioridad con cuidado especial de UI (§7),
 * nunca con una alarma agresiva.
 */
enum class TriageSeverity { SAFE, CAUTION, DANGER, DECEASED_CARE }

class TriageHapticPlayer(private val context: Context) {

    fun play(severity: TriageSeverity) {
        val vibrator = getVibrator() ?: return
        val effect = when (severity) {
            TriageSeverity.SAFE -> VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            TriageSeverity.CAUTION -> VibrationEffect.createWaveform(
                longArrayOf(0, 80, 80, 80), -1
            )
            TriageSeverity.DANGER -> VibrationEffect.createWaveform(
                longArrayOf(0, 120, 60, 120, 60, 120), -1
            )
            TriageSeverity.DECEASED_CARE -> VibrationEffect.createOneShot(40, 60)
        }
        vibrator.vibrate(effect)
    }

    private fun getVibrator(): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()
}

@Composable
fun rememberTriageHapticPlayer(): TriageHapticPlayer {
    val context = LocalContext.current
    return remember(context) { TriageHapticPlayer(context) }
}
