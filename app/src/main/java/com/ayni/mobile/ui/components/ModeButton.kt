package com.ayni.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing

/**
 * Botón grande de Home (MÉDICO / ESTRUCTURA). "Aún más grande" que el tap target
 * mínimo de 56dp (§6.1) — aquí 128dp de alto, glanceable, sin ambigüedad de qué toca.
 */
@Composable
fun ModeButton(
    label: String,
    description: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .semantics { contentDescription = "$label. $description" },
        shape = AyniShapes.large,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(PaddingValues(Spacing.md)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )
            Text(text = label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
