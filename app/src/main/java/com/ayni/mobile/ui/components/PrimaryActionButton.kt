package com.ayni.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.MinTapTarget

/**
 * Botón grande genérico (>=56dp, §6.1) para acciones primarias en la mitad inferior de
 * la pantalla (thumb zone): capturar, enviar, "nuevo análisis", etc.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    val descriptionText = contentDescription
    val describedModifier = if (descriptionText != null) {
        modifier.semantics { this.contentDescription = descriptionText }
    } else {
        modifier
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = AyniShapes.medium,
        modifier = describedModifier
            .fillMaxWidth()
            .height(MinTapTarget)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
