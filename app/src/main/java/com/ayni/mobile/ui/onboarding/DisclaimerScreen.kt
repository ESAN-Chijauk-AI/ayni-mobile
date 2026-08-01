package com.ayni.mobile.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.theme.Signal
import com.ayni.mobile.ui.theme.Spacing

/**
 * §7 del spec: disclaimer claro y visible (onboarding obligatorio la primera vez +
 * accesible desde resultado). Un solo texto fuente de verdad para ambos casos.
 */
@Composable
fun DisclaimerScreen(
    onContinue: () -> Unit,
    viewModel: DisclaimerViewModel = hiltViewModel()
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = Icons.Filled.PrivacyTip,
                contentDescription = null,
                tint = Signal
            )
            Text(
                text = stringResource(R.string.disclaimer_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.disclaimer_body_purpose),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.disclaimer_body_privacy),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.disclaimer_body_caution),
                style = MaterialTheme.typography.bodyLarge
            )

            PrimaryActionButton(
                text = stringResource(R.string.disclaimer_action_acknowledge),
                onClick = {
                    viewModel.onAcknowledge()
                    onContinue()
                }
            )
        }
    }
}
