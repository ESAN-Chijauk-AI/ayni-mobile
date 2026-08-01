package com.ayni.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayni.mobile.R
import com.ayni.mobile.ui.navigation.AyniDestinations
import com.ayni.mobile.ui.theme.AyniOutlineVariant
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniSecondary
import com.ayni.mobile.ui.theme.AyniSurfaceContainerLowest

enum class AyniTab { SOS, MEDICAL, STRUCTURAL, PROXIMITY }

/** Rutas visibles con bottom nav y a qué tab corresponden (4 tabs: Inicio/Médico/Estructura/Proximidad). */
fun routeToTab(route: String?): AyniTab? = when (route) {
    AyniDestinations.HOME -> AyniTab.SOS
    AyniDestinations.MEDICAL_GRAPH, AyniDestinations.MEDICAL_INPUT, AyniDestinations.MEDICAL_RESULT -> AyniTab.MEDICAL
    AyniDestinations.STRUCTURAL_GRAPH, AyniDestinations.STRUCTURAL_CAPTURE, AyniDestinations.STRUCTURAL_RESULT -> AyniTab.STRUCTURAL
    AyniDestinations.PROXIMITY -> AyniTab.PROXIMITY
    else -> null
}

/**
 * Barra flotante glassmorphic de 4 tabs: Inicio (SOS) / Médico / Estructura / Proximidad.
 * Sin blur real (minSdk 26, RenderEffect pediría API 31+) — se aproxima con superficie
 * semitransparente + sombra. `windowInsetsPadding(WindowInsets.navigationBars)` evita que
 * la barra del sistema (gestos o 3 botones) la tape.
 */
@Composable
fun AyniBottomNav(
    selected: AyniTab,
    onSosClick: () -> Unit,
    onMedicalClick: () -> Unit,
    onStructuralClick: () -> Unit,
    onProximityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 22.dp)
            .widthIn(max = 500.dp)
            .fillMaxWidth(),
        shape = CircleShape,
        color = AyniSurfaceContainerLowest.copy(alpha = 0.92f),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, AyniOutlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    label = stringResource(R.string.nav_tab_sos),
                    icon = Icons.Filled.Sos,
                    isSelected = selected == AyniTab.SOS,
                    onClick = onSosClick
                )
                BottomNavItem(
                    label = stringResource(R.string.nav_tab_medical),
                    icon = Icons.Filled.MedicalServices,
                    isSelected = selected == AyniTab.MEDICAL,
                    onClick = onMedicalClick
                )
                BottomNavItem(
                    label = stringResource(R.string.nav_tab_structural),
                    icon = Icons.Filled.PhotoCamera,
                    isSelected = selected == AyniTab.STRUCTURAL,
                    onClick = onStructuralClick
                )
                BottomNavItem(
                    label = stringResource(R.string.nav_tab_proximity),
                    icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                    isSelected = selected == AyniTab.PROXIMITY,
                    onClick = onProximityClick
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) AyniPrimaryContainer else AyniSecondary
    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .semantics {
                contentDescription = label
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isSelected) AyniPrimaryFixed.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Transparent
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Padding inferior para que el contenido scrolleable no quede tapado por la barra flotante. */
val BottomNavClearance = PaddingValues(bottom = 96.dp)
