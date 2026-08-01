package com.ayni.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayni.mobile.domain.model.MedicalPriority
import com.ayni.mobile.domain.model.StructuralVerdict
import com.ayni.mobile.ui.theme.AyniSemanticColors
import com.ayni.mobile.ui.theme.Spacing

/**
 * Representación visual + accesible de un estado del semáforo. Nunca depende solo del
 * color (§6.4): siempre trae palabra + ícono + severidad para haptic.
 */
data class VerdictDisplay(
    val backgroundColor: Color,
    val contentColor: Color,
    val label: String,
    val icon: ImageVector,
    val severity: TriageSeverity
)

fun StructuralVerdict.toDisplay(): VerdictDisplay = when (this) {
    StructuralVerdict.VERDE -> VerdictDisplay(
        AyniSemanticColors.verde, Color.Black, "SEGURO", Icons.Filled.CheckCircle, TriageSeverity.SAFE
    )
    StructuralVerdict.AMARILLO -> VerdictDisplay(
        AyniSemanticColors.amarillo, Color.Black, "PRECAUCIÓN", Icons.Filled.Warning, TriageSeverity.CAUTION
    )
    StructuralVerdict.ROJO -> VerdictDisplay(
        AyniSemanticColors.rojo, Color.White, "EVACÚA", Icons.Filled.Error, TriageSeverity.DANGER
    )
}

fun MedicalPriority.toDisplay(): VerdictDisplay = when (this) {
    MedicalPriority.VERDE -> VerdictDisplay(
        AyniSemanticColors.verde, Color.Black, "LEVE", Icons.Filled.CheckCircle, TriageSeverity.SAFE
    )
    MedicalPriority.AMARILLO -> VerdictDisplay(
        AyniSemanticColors.amarillo, Color.Black, "URGENTE", Icons.Filled.Warning, TriageSeverity.CAUTION
    )
    MedicalPriority.ROJO -> VerdictDisplay(
        AyniSemanticColors.rojo, Color.White, "CRÍTICO", Icons.Filled.Error, TriageSeverity.DANGER
    )
    MedicalPriority.NEGRO -> VerdictDisplay(
        AyniSemanticColors.negro, AyniSemanticColors.negroLabel, "PRIORIDAD ESPECIAL", Icons.Filled.HealthAndSafety, TriageSeverity.DECEASED_CARE
    )
}

/**
 * Color dominante a pantalla completa (o al área que le dé el caller) + palabra + ícono,
 * entendible en <1s (§6.1). Dispara el patrón háptico correspondiente una sola vez al
 * aparecer. NEGRO nunca usa aquí un ícono/copy frío — "PRIORIDAD ESPECIAL" en vez de
 * un rótulo clínico, siguiendo la exigencia de cuidado del spec §7.
 */
@Composable
fun VerdictSemaphore(
    display: VerdictDisplay,
    modifier: Modifier = Modifier
) {
    val haptics = rememberTriageHapticPlayer()
    LaunchedEffect(display.severity) {
        haptics.play(display.severity)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(display.backgroundColor)
            .padding(Spacing.lg)
            .semantics { contentDescription = "Veredicto: ${display.label}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = display.icon,
            contentDescription = null, // el contentDescription ya está en el contenedor
            tint = display.contentColor,
            modifier = Modifier.padding(bottom = Spacing.md)
        )
        Text(
            text = display.label,
            color = display.contentColor,
            style = MaterialTheme.typography.displayMedium
        )
    }
}
