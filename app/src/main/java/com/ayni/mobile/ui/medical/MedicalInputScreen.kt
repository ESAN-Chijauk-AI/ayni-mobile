package com.ayni.mobile.ui.medical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.InjuryChip
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.theme.Spacing

/**
 * F3: input rápido de lesión. Chips de lesiones comunes primero (preferidas sobre
 * teclear, §3/F3), texto libre opcional para casos que no calzan en los botones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalInputScreen(
    parentEntry: NavBackStackEntry,
    onResultReady: () -> Unit
) {
    val viewModel: MedicalViewModel = hiltViewModel(parentEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is MedicalUiState.Result) onResultReady()
    }

    val input = uiState as? MedicalUiState.Input
    val analyzing = uiState is MedicalUiState.Analyzing
    val errorMessage = (uiState as? MedicalUiState.Error)?.message

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.lg)
        ) {
            Text(
                text = stringResource(R.string.medical_input_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.medical_input_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                COMMON_INJURIES.forEach { label ->
                    InjuryChip(
                        label = label,
                        selected = input?.selectedChips?.contains(label) == true,
                        onClick = { viewModel.onToggleChip(label) }
                    )
                }
            }

            OutlinedTextField(
                value = input?.freeText.orEmpty(),
                onValueChange = viewModel::onFreeTextChange,
                label = { Text(stringResource(R.string.medical_input_free_text_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                PrimaryActionButton(
                    text = if (analyzing) {
                        stringResource(R.string.medical_input_analyzing)
                    } else {
                        stringResource(R.string.medical_input_submit)
                    },
                    enabled = !analyzing && input != null &&
                        (input.selectedChips.isNotEmpty() || input.freeText.isNotBlank()),
                    onClick = viewModel::onSubmit
                )
            }
        }
    }
}
