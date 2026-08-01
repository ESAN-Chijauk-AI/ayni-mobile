package com.ayni.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ayni.mobile.R
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Signal
import com.ayni.mobile.ui.theme.Spacing
import com.ayni.mobile.ui.theme.SurfaceRaised

/**
 * Banner compacto reusable (§7: disclaimer accesible desde onboarding y desde resultado).
 * onVerMas navega a DisclaimerScreen con el detalle completo.
 */
@Composable
fun DisclaimerBanner(
    onVerMas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AyniShapes.medium,
        color = SurfaceRaised
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Signal
            )
            Text(
                text = stringResource(R.string.disclaimer_banner_text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.sm)
            )
            TextButton(onClick = onVerMas) {
                Text(stringResource(R.string.disclaimer_banner_action))
            }
        }
    }
}
