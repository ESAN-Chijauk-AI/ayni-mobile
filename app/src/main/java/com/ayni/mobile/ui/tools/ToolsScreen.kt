package com.ayni.mobile.ui.tools

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing

/**
 * F1' (stitch_remix_of_ayni_mobile_emergency_response/herramientas_de_supervivencia).
 * Grid de accesos rápidos. "Primeros Auxilios" es el único con backend real hoy: entra
 * al triage médico con Gemma que ya funciona. Linterna/Señal Sonora/Brújula quedan como
 * UI sin implementar (a pedido explícito: "lo que no tengamos, funcionar acorde al UI y
 * ya" — nada de fingir que prenden hardware que no está cableado). "Estado del sensor" y
 * "Monitoreo" (nodo ESP32 real, subsistema IoT) y "Red de proximidad" (SOS por BLE cercano)
 * sí funcionan y viven aquí para no ocupar un tab propio en la bottom nav de 4 elementos.
 */
@Composable
fun ToolsScreen(
    onPrimerosAuxiliosClick: () -> Unit,
    onSensorStatusClick: () -> Unit,
    onMonitoringClick: () -> Unit,
    onProximityClick: () -> Unit
) {
    val context = LocalContext.current
    val notImplemented = stringResource(R.string.action_not_implemented)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.marginPage)
            .padding(BottomNavClearance)
    ) {
        Text(
            text = stringResource(R.string.tools_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.lg)
        )

        val items = toolItems(context, notImplemented, onPrimerosAuxiliosClick)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.weight(1f)
        ) {
            items(items) { item ->
                ToolCard(item)
            }
        }

        TextButton(onClick = onSensorStatusClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tools_sensor_status_link))
        }
        TextButton(onClick = onMonitoringClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nav_monitoring))
        }
        TextButton(onClick = onProximityClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tools_proximity_link))
        }

        Surface(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md, bottom = Spacing.lg)
                .height(56.dp),
            shape = AyniShapes.large,
            color = MaterialTheme.colorScheme.error
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.Emergency, contentDescription = null, tint = Color.White)
                Text(
                    text = stringResource(R.string.tools_activate_sos),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
        }
    }
}

private data class ToolItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun toolItems(
    context: android.content.Context,
    notImplemented: String,
    onPrimerosAuxiliosClick: () -> Unit
): List<ToolItem> = listOf(
    ToolItem(stringResource(R.string.tools_flashlight), Icons.Filled.FlashlightOn) {
        Toast.makeText(context, notImplemented, Toast.LENGTH_SHORT).show()
    },
    ToolItem(stringResource(R.string.tools_sound_signal), Icons.Filled.SettingsInputAntenna) {
        Toast.makeText(context, notImplemented, Toast.LENGTH_SHORT).show()
    },
    ToolItem(stringResource(R.string.tools_compass), Icons.Filled.Explore) {
        Toast.makeText(context, notImplemented, Toast.LENGTH_SHORT).show()
    },
    ToolItem(stringResource(R.string.tools_first_aid), Icons.Filled.MedicalServices, onPrimerosAuxiliosClick)
)

@Composable
private fun ToolCard(item: ToolItem) {
    Surface(
        onClick = item.onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = AyniShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, AyniHairline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = AyniPrimaryFixed.copy(alpha = 0.5f), modifier = Modifier.size(64.dp)) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = item.icon, contentDescription = null, tint = AyniPrimaryContainer)
                }
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.sm, start = Spacing.xs, end = Spacing.xs)
            )
        }
    }
}
