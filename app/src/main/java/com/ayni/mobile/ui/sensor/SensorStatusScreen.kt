package com.ayni.mobile.ui.sensor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.SensorSignatureReadout
import com.ayni.mobile.ui.theme.Spacing

/**
 * F4 (mock por ahora, ver data/sensor/MockSensorRepository): estado de conexión
 * siempre visible + readout signature + "Simular réplica" (mismo botón mágico del
 * resultado estructural, aquí para poder probarlo sin estar a mitad de un análisis).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorStatusScreen(
    onBack: () -> Unit,
    viewModel: SensorStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sensor_status_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.lg)
        ) {
            Text(
                text = if (uiState.isSimulationMode) {
                    stringResource(R.string.sensor_status_simulation_notice)
                } else {
                    stringResource(R.string.sensor_status_hardware_notice)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = Spacing.md)
            )

            SensorSignatureReadout(
                magnitudes = uiState.magnitudes,
                connected = uiState.connected,
                isSimulated = uiState.isSimulationMode
            )

            uiState.lastReading?.let { reading ->
                Column(modifier = Modifier.padding(top = Spacing.md)) {
                    Text(
                        text = "ax=%.2f  ay=%.2f  az=%.2f".format(reading.ax, reading.ay, reading.az),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                TextButton(onClick = viewModel::onSimulateAftershock) {
                    Text(stringResource(R.string.structural_result_simulate_aftershock))
                }
                TextButton(onClick = viewModel::onToggleConnection) {
                    Text(
                        if (uiState.connected) {
                            stringResource(R.string.sensor_status_disconnect)
                        } else {
                            stringResource(R.string.sensor_status_connect)
                        }
                    )
                }
            }
        }
    }
}
