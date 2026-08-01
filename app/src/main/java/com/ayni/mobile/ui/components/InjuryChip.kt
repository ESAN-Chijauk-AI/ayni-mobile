package com.ayni.mobile.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Chip seleccionable para lesiones comunes. Preferir botones sobre teclear (§3/F3:
 * "más rápido y usable en pánico"). Tap target 48dp: FilterChip de M3 ya cumple el
 * mínimo táctil recomendado para controles secundarios (los primarios usan 56dp+).
 */
@Composable
fun InjuryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.height(48.dp),
        colors = FilterChipDefaults.filterChipColors()
    )
}
