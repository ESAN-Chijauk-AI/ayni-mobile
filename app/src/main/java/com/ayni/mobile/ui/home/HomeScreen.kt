package com.ayni.mobile.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.AiStatus
import com.ayni.mobile.ui.components.OfflineStatusBadge
import com.ayni.mobile.ui.components.ModeButton
import com.ayni.mobile.ui.theme.AyniSemanticColors
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing
import com.ayni.mobile.ui.theme.SurfaceRaised

/**
 * F1: pantalla de entrada. Dos botones grandes en la mitad inferior (thumb zone, §6.1),
 * sin scroll, acceso a estado del sensor. El disclaimer completo vive en su propia
 * pantalla (accesible aquí y desde resultado); esta pantalla solo muestra el badge de
 * estado de IA para no competir visualmente con los dos botones de modo.
 */
@Composable
fun HomeScreen(
    onEstructuralClick: () -> Unit,
    onMedicoClick: () -> Unit,
    onSensorStatusClick: () -> Unit,
    onMonitoringClick: () -> Unit,
    onProximityClick: () -> Unit,
    onDisclaimerClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onModelFileSelected) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.Top
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                    OfflineStatusBadge(
                        aiStatus = when {
                            uiState.aiReady -> AiStatus.READY
                            !uiState.modelFilePresent -> AiStatus.MODEL_MISSING
                            else -> AiStatus.WARMING_UP
                        }
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                TextButton(onClick = onDisclaimerClick) {
                    Text(stringResource(R.string.home_disclaimer_link))
                }

                if (uiState.isImportingModel) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.sm),
                        shape = AyniShapes.medium,
                        color = SurfaceRaised
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                            Text(
                                text = stringResource(R.string.home_model_importing),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = Spacing.sm)
                            )
                        }
                    }
                } else if (!uiState.modelFilePresent) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.sm),
                        shape = AyniShapes.medium,
                        color = SurfaceRaised
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(
                                text = if (uiState.importError) {
                                    stringResource(R.string.home_model_import_error)
                                } else {
                                    stringResource(R.string.home_model_missing_body)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(
                                onClick = { modelPickerLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.padding(top = Spacing.xs)
                            ) {
                                Text(stringResource(R.string.home_model_missing_action))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ModeButton(
                    label = stringResource(R.string.home_mode_proximity),
                    description = stringResource(R.string.home_mode_proximity_description),
                    icon = Icons.Filled.Warning,
                    containerColor = AyniSemanticColors.rojo,
                    contentColor = Color.White,
                    onClick = onProximityClick
                )
                ModeButton(
                    label = stringResource(R.string.home_mode_structural),
                    description = stringResource(R.string.home_mode_structural_description),
                    icon = Icons.Filled.Domain,
                    containerColor = AyniSemanticColors.signal,
                    contentColor = Color.Black,
                    onClick = onEstructuralClick
                )
                ModeButton(
                    label = stringResource(R.string.home_mode_medical),
                    description = stringResource(R.string.home_mode_medical_description),
                    icon = Icons.Filled.HealthAndSafety,
                    containerColor = AyniSemanticColors.rojo,
                    contentColor = Color.White,
                    onClick = onMedicoClick
                )

                Surface(
                    onClick = onSensorStatusClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (uiState.sensorConnected) {
                                stringResource(R.string.home_sensor_connected)
                            } else {
                                stringResource(R.string.home_sensor_disconnected)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!uiState.sensorConnected) {
                            Icon(
                                imageVector = Icons.Filled.SensorsOff,
                                contentDescription = null
                            )
                        }
                    }
                }

                // Acceso al monitoreo estructural del nodo ESP32 (subsistema IoT migrado).
                Surface(
                    onClick = onMonitoringClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.nav_monitoring),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
