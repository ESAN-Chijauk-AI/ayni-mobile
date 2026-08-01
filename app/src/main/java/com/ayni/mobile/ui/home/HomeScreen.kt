package com.ayni.mobile.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.AiStatus
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.components.OfflineStatusBadge
import com.ayni.mobile.ui.theme.AyniBrandSoft
import com.ayni.mobile.ui.theme.AyniDangerRed
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniInputBackground
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * F1 rediseñado (stitch_remix_of_ayni_mobile_emergency_response/inicio_de_emergencia_sos):
 * Home pasa de "elegir modo" a un centro de **SOS**. Los modos ESTRUCTURA/MÉDICO viven
 * ahora detrás de la pestaña Inspección y de la tarjeta "Primeros Auxilios" en
 * Herramientas (ver AyniBottomNav/ToolsScreen) — Home es la superficie de emergencia.
 *
 * Real: mantener 3s el botón SOS abre el marcador con el 911 (Intent.ACTION_DIAL, no
 * requiere permiso porque no auto-llama). "Estoy Atrapado"/"Enviar Ubicación" y el
 * contacto ICE quedan como UI sin backend todavía (no hay servicio de ubicación/SMS
 * implementado hoy) — se lo dice explícito al usuario en vez de simular que funcionan.
 */
@Composable
fun HomeScreen(
    onDisclaimerClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onModelFileSelected) }

    val notImplementedMessage = stringResource(R.string.action_not_implemented)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.marginPage, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            OfflineStatusBadge(
                aiStatus = when {
                    uiState.aiReady -> AiStatus.READY
                    !uiState.modelFilePresent -> AiStatus.MODEL_MISSING
                    else -> AiStatus.WARMING_UP
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.marginPage)
                .padding(BottomNavClearance),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                TextButton(onClick = onDisclaimerClick) {
                    Text(stringResource(R.string.home_disclaimer_link))
                }

                ModelStatusBanner(
                    isImporting = uiState.isImportingModel,
                    modelPresent = uiState.modelFilePresent,
                    importError = uiState.importError,
                    onPickModel = { modelPickerLauncher.launch(arrayOf("*/*")) }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    SosButton(
                        onActivate = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                            runCatching { context.startActivity(intent) }
                        }
                    )
                    Text(
                        text = stringResource(R.string.home_sos_hold_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SecondaryActionCard(
                        label = stringResource(R.string.home_action_trapped),
                        icon = Icons.Filled.PanTool,
                        modifier = Modifier.weight(1f),
                        onClick = { Toast.makeText(context, notImplementedMessage, Toast.LENGTH_SHORT).show() }
                    )
                    SecondaryActionCard(
                        label = stringResource(R.string.home_action_send_location),
                        icon = Icons.Filled.LocationOn,
                        modifier = Modifier.weight(1f),
                        onClick = { Toast.makeText(context, notImplementedMessage, Toast.LENGTH_SHORT).show() }
                    )
                }

                Text(
                    text = stringResource(R.string.home_emergency_contacts_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AyniShapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(0.5.dp, AyniHairline)
                ) {
                    Column {
                        EmergencyContactRow(
                            icon = Icons.Filled.Shield,
                            title = "911",
                            subtitle = stringResource(R.string.home_contact_911_subtitle),
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                                runCatching { context.startActivity(intent) }
                            }
                        )
                        androidx.compose.material3.HorizontalDivider(color = AyniHairline, thickness = 0.5.dp)
                        EmergencyContactRow(
                            icon = Icons.Filled.Badge,
                            title = stringResource(R.string.home_contact_ice_title),
                            subtitle = stringResource(R.string.home_contact_ice_subtitle),
                            onCall = { Toast.makeText(context, notImplementedMessage, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelStatusBanner(
    isImporting: Boolean,
    modelPresent: Boolean,
    importError: Boolean,
    onPickModel: () -> Unit
) {
    if (isImporting) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            shape = AyniShapes.medium,
            color = AyniInputBackground
        ) {
            Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.home_model_importing),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
        }
    } else if (!modelPresent) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            shape = AyniShapes.medium,
            color = AyniInputBackground
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = if (importError) {
                        stringResource(R.string.home_model_import_error)
                    } else {
                        stringResource(R.string.home_model_missing_body)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onPickModel, modifier = Modifier.padding(top = Spacing.xs)) {
                    Text(stringResource(R.string.home_model_missing_action))
                }
            }
        }
    }
}

/** Botón SOS circular. Mantener 3s dispara onActivate (marcador al 911). */
@Composable
private fun SosButton(onActivate: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var pressProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(220.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val job = coroutineScope.launch {
                        val steps = 30
                        repeat(steps) { i ->
                            pressProgress = (i + 1f) / steps
                            kotlinx.coroutines.delay(3000L / steps)
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onActivate()
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    pressProgress = 0f
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = AyniBrandSoft)
            if (pressProgress > 0f) {
                drawArc(
                    color = AyniPrimaryContainer,
                    startAngle = -90f,
                    sweepAngle = 360f * pressProgress,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(172.dp)
                .background(AyniDangerRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Sos,
                    contentDescription = stringResource(R.string.home_sos_content_description),
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(R.string.home_sos_label),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SecondaryActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.1f),
        shape = AyniShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, AyniHairline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AyniPrimaryFixed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AyniPrimaryContainer)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.sm, start = Spacing.xs, end = Spacing.xs)
            )
        }
    }
}

@Composable
private fun EmergencyContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.padding(start = Spacing.md)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            onClick = onCall,
            shape = CircleShape,
            color = AyniInputBackground,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Filled.Call, contentDescription = title, tint = AyniPrimaryContainer)
            }
        }
    }
}
