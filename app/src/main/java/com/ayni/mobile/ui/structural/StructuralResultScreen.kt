package com.ayni.mobile.ui.structural

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.ayni.mobile.R
import com.ayni.mobile.domain.model.StructuralResult
import com.ayni.mobile.domain.model.StructuralVerdict
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.components.SensorSignatureReadout
import com.ayni.mobile.ui.components.TriageSeverity
import com.ayni.mobile.ui.components.rememberTriageHapticPlayer
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Negro
import com.ayni.mobile.ui.theme.Rojo
import com.ayni.mobile.ui.theme.Spacing
import com.ayni.mobile.ui.theme.Verde
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * F5, restyle stitch_remix_of_ayni_mobile_emergency_response/reporte_estructural. El
 * veredicto ya no es pantalla completa de color (patrón viejo) — es un badge de riesgo +
 * foto real + tarjetas de datos, más fiel al mockup. El color semántico (verde/amarillo/
 * rojo, §6.2 del spec original) sigue intocable: solo cambia dónde se pinta, no el hex.
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
    val capturedImage by viewModel.capturedImage.collectAsStateWithLifecycle()

    val result = (uiState as? StructuralUiState.Result)?.result ?: return

    StructuralReportContent(
        result = result,
        imageBytes = capturedImage,
        timestampMs = System.currentTimeMillis(),
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
fun StructuralReportContent(
    result: StructuralResult,
    imageBytes: ByteArray?,
    timestampMs: Long,
    sensorConnected: Boolean,
    magnitudes: List<Float>,
    onSimulateAftershock: () -> Unit,
    onNewAnalysis: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberTriageHapticPlayer()
    val severity = when (result.verdict) {
        StructuralVerdict.VERDE -> TriageSeverity.SAFE
        StructuralVerdict.AMARILLO -> TriageSeverity.CAUTION
        StructuralVerdict.ROJO -> TriageSeverity.DANGER
    }
    LaunchedEffect(result.verdict) { haptics.play(severity) }

    val badgeColor = when (result.verdict) {
        StructuralVerdict.VERDE -> Verde
        StructuralVerdict.AMARILLO -> Color(0xFFF5B400)
        StructuralVerdict.ROJO -> Rojo
    }
    val badgeLabel = when (result.verdict) {
        StructuralVerdict.VERDE -> stringResource(R.string.structural_badge_safe)
        StructuralVerdict.AMARILLO -> stringResource(R.string.structural_badge_caution)
        StructuralVerdict.ROJO -> stringResource(R.string.structural_badge_high_risk)
    }
    val badgeTextColor = if (result.verdict == StructuralVerdict.AMARILLO) Negro else Color.White

    val bitmap = remember(imageBytes) {
        imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.marginPage)
            .padding(BottomNavClearance)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(stringResource(R.string.structural_report_title), style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = "ID: ${result.verdict}-${timestampMs.toString().takeLast(6)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(shape = AyniShapes.large, color = badgeColor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = badgeTextColor, modifier = Modifier.size(16.dp))
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeTextColor,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = AyniShapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(0.5.dp, AyniHairline)
                ) {
                    Column {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.structural_report_no_photo),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(modifier = Modifier.padding(Spacing.lg)) {
                            Icon(imageVector = Icons.Filled.SmartToy, contentDescription = null, tint = badgeColor)
                            Text(
                                text = "${result.razon}. ${result.accion}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = Spacing.sm)
                            )
                        }
                    }
                }
            }

            item {
                DataRowCard(
                    icon = Icons.Filled.CalendarMonth,
                    label = stringResource(R.string.structural_report_datetime_label),
                    value = SimpleDateFormat("d MMM yyyy · HH:mm", Locale("es")).format(Date(timestampMs))
                )
            }
            item {
                DataRowCard(
                    icon = Icons.Filled.LocationOn,
                    label = stringResource(R.string.structural_report_gps_label),
                    value = stringResource(R.string.structural_report_gps_unavailable)
                )
            }

            item {
                Surface(
                    shape = AyniShapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(0.5.dp, AyniHairline)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(stringResource(R.string.structural_report_pulse_title), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(Spacing.sm))
                        SensorSignatureReadout(
                            magnitudes = magnitudes,
                            connected = sensorConnected,
                            isSimulated = true
                        )
                        TextButton(onClick = onSimulateAftershock) {
                            Text(stringResource(R.string.structural_result_simulate_aftershock))
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = AyniShapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(0.5.dp, AyniHairline)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            stringResource(R.string.structural_report_observations_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )
                        ObservationRow(icon = Icons.Filled.Warning, tint = badgeColor, text = result.razon)
                        HorizontalDivider(color = AyniHairline, thickness = 0.5.dp)
                        ObservationRow(icon = Icons.Filled.Info, tint = MaterialTheme.colorScheme.onSurfaceVariant, text = result.accion)
                        if (!result.usoSensor) {
                            HorizontalDivider(color = AyniHairline, thickness = 0.5.dp)
                            ObservationRow(
                                icon = Icons.Filled.CheckCircle,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = stringResource(R.string.structural_result_no_sensor)
                            )
                        }
                    }
                }
            }

            item {
                androidx.compose.material3.Button(
                    onClick = {
                        val shareText = "${result.razon}. ${result.accion}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = AyniShapes.medium
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                    Text(
                        text = stringResource(R.string.structural_report_share),
                        modifier = Modifier.padding(start = Spacing.sm)
                    )
                }
            }

            item {
                androidx.compose.material3.OutlinedButton(
                    onClick = onNewAnalysis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = AyniShapes.medium
                ) {
                    Text(stringResource(R.string.structural_result_new_analysis))
                }
            }

            item {
                TextButton(onClick = onDisclaimerClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_disclaimer_link))
                }
            }
        }
    }
}

@Composable
private fun DataRowCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(
        shape = AyniShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, AyniHairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(Modifier),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(modifier = Modifier.padding(start = Spacing.md)) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ObservationRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = tint.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = Spacing.md))
    }
}
