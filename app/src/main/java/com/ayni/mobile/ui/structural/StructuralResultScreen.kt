package com.ayni.mobile.ui.structural

import androidx.compose.foundation.layout.Arrangement
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
import com.ayni.mobile.domain.model.StructuralResult
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.components.SensorSignatureReadout
import com.ayni.mobile.ui.components.VerdictSemaphore
import com.ayni.mobile.ui.components.toDisplay
import com.ayni.mobile.ui.theme.Spacing

/**
 * F5: veredicto a pantalla completa con color dominante (VerdictSemaphore), razón y
 * acción cortas, el readout del sensor que respaldó el análisis (§6.3: fusión
 * sensor+IA visible), acceso al disclaimer, y "Nuevo análisis" (resetea la sesión).
 */
@Composable
fun StructuralResultScreen(
    parentEntry: NavBackStackEntry,
    onNewAnalysis: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    val viewModel: StructuralViewModel = hiltViewModel(parentEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sensorConnected by viewModel.sensorConnected.collectAsStateWithLifecycle()
    val magnitudes by viewModel.magnitudes.collectAsStateWithLifecycle()

    val result = (uiState as? StructuralUiState.Result)?.result ?: return

    StructuralResultContent(
        result = result,
        sensorConnected = sensorConnected,
        magnitudes = magnitudes,
        onSimulateAftershock = viewModel::onSimulateAftershock,
        onNewAnalysis = {
            viewModel.onNewAnalysis()
            onNewAnalysis()
        },
        onDisclaimerClick = onDisclaimerClick
    )
}

@Composable
private fun StructuralResultContent(
    result: StructuralResult,
    sensorConnected: Boolean,
    magnitudes: List<Float>,
    onSimulateAftershock: () -> Unit,
    onNewAnalysis: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    val display = result.verdict.toDisplay()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            VerdictSemaphore(display = display, modifier = Modifier.fillMaxSize())
        }

        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(text = result.razon, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = result.accion,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Spacing.xs)
                )

                if (!result.usoSensor) {
                    Text(
                        text = stringResource(R.string.structural_result_no_sensor),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }

                Column(modifier = Modifier.padding(top = Spacing.md)) {
                    SensorSignatureReadout(
                        magnitudes = magnitudes,
                        connected = sensorConnected,
                        isSimulated = true
                    )
                }

                TextButton(onClick = onSimulateAftershock) {
                    Text(stringResource(R.string.structural_result_simulate_aftershock))
                }

                PrimaryActionButton(
                    text = stringResource(R.string.structural_result_new_analysis),
                    onClick = onNewAnalysis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm)
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
