package com.ayni.mobile.ui.medical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.ayni.mobile.R
import com.ayni.mobile.domain.model.MedicalResult
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.components.TriageSeverity
import com.ayni.mobile.ui.components.VerdictSemaphore
import com.ayni.mobile.ui.components.toDisplay
import com.ayni.mobile.ui.theme.Spacing

/**
 * F5 (médico): prioridad START a pantalla completa + hasta 3 pasos de primeros
 * auxilios. NEGRO nunca se muestra frío (§7): toDisplay() ya lo etiqueta "PRIORIDAD
 * ESPECIAL" con copy que dirige a ayuda profesional (ver texto fijo abajo).
 */
@Composable
fun MedicalResultScreen(
    parentEntry: NavBackStackEntry,
    onNewAnalysis: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    val viewModel: MedicalViewModel = hiltViewModel(parentEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val result = (uiState as? MedicalUiState.Result)?.result ?: return

    MedicalResultContent(
        result = result,
        onNewAnalysis = {
            viewModel.onNewAnalysis()
            onNewAnalysis()
        },
        onDisclaimerClick = onDisclaimerClick
    )
}

@Composable
private fun MedicalResultContent(
    result: MedicalResult,
    onNewAnalysis: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    val display = result.prioridad.toDisplay()
    val isDeceasedCare = display.severity == TriageSeverity.DECEASED_CARE

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            VerdictSemaphore(display = display, modifier = Modifier.fillMaxSize())
        }

        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                if (isDeceasedCare) {
                    Text(
                        text = stringResource(R.string.medical_result_negro_guidance),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }

                Text(
                    text = stringResource(R.string.medical_result_steps_title),
                    style = MaterialTheme.typography.titleLarge
                )
                result.primerosAuxilios.forEach { paso ->
                    Text(
                        text = "• $paso",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }

                Text(
                    text = stringResource(R.string.medical_result_no_diagnosis),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.md)
                )

                PrimaryActionButton(
                    text = stringResource(R.string.structural_result_new_analysis),
                    onClick = onNewAnalysis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md)
                )

                TextButton(
                    onClick = onDisclaimerClick,
                    modifier = Modifier.padding(top = Spacing.xs)
                ) {
                    Text(stringResource(R.string.home_disclaimer_link))
                }
            }
        }
    }
}
