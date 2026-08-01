package com.ayni.mobile.ui.structural

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.theme.Spacing

/**
 * Pestaña "Reportes" del bottom nav (rediseño Stitch): muestra el último análisis
 * estructural completado en la sesión (ReportsViewModel lee LastStructuralReportState).
 * Vacío hasta que se haga una inspección — no es F7 (historial persistente).
 */
@Composable
fun ReportsScreen(
    onNewAnalysis: () -> Unit,
    onDisclaimerClick: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val report by viewModel.lastReport.collectAsStateWithLifecycle()
    val sensorConnected by viewModel.sensorConnected.collectAsStateWithLifecycle()
    val magnitudes by viewModel.magnitudes.collectAsStateWithLifecycle()

    val current = report
    if (current == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.marginPage)
                .padding(BottomNavClearance),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.reports_empty_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.reports_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }
    } else {
        StructuralReportContent(
            result = current.result,
            imageBytes = current.imageBytes,
            timestampMs = current.timestampMs,
            sensorConnected = sensorConnected,
            magnitudes = magnitudes,
            onSimulateAftershock = viewModel::onSimulateAftershock,
            onNewAnalysis = onNewAnalysis,
            onDisclaimerClick = onDisclaimerClick
        )
    }
}
