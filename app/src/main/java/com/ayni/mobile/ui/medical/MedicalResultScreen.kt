package com.ayni.mobile.ui.medical

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.ayni.mobile.R
import com.ayni.mobile.domain.model.MedicalResult
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.components.TriageSeverity
import com.ayni.mobile.ui.components.VerdictSemaphore
import com.ayni.mobile.ui.components.toDisplay
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniOnPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing

/**
 * F5 (médico): prioridad START + hasta 3 pasos de primeros auxilios. NEGRO nunca se
 * muestra frío (§7): toDisplay() ya lo etiqueta "PRIORIDAD ESPECIAL" con copy que dirige a
 * ayuda profesional. El semáforo (color+ícono+palabra+haptic, §6.4) queda como una franja
 * fija arriba en vez de dominar toda la pantalla — deja espacio real para las
 * recomendaciones, que son la parte accionable. Todo el contenido de abajo es scrolleable
 * con clearance para el nav flotante, que antes tapaba el final de la lista y el botón.
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

private val VERDICT_BANNER_HEIGHT = 210.dp

@Composable
private fun MedicalResultContent(
    result: MedicalResult,
    onNewAnalysis: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    val display = result.prioridad.toDisplay()
    val isDeceasedCare = display.severity == TriageSeverity.DECEASED_CARE

    Column(modifier = Modifier.fillMaxSize()) {
        VerdictSemaphore(display = display, modifier = Modifier.height(VERDICT_BANNER_HEIGHT))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg)
                .padding(BottomNavClearance)
        ) {
            if (isDeceasedCare) {
                Text(
                    text = stringResource(R.string.medical_result_negro_guidance),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
            }

            Text(
                text = stringResource(R.string.medical_result_steps_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            result.primerosAuxilios.forEachIndexed { index, paso ->
                RecommendationCard(step = index + 1, text = paso)
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Text(
                text = stringResource(R.string.medical_result_no_diagnosis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm)
            )

            PrimaryActionButton(
                text = stringResource(R.string.structural_result_new_analysis),
                onClick = onNewAnalysis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg)
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

@Composable
private fun RecommendationCard(step: Int, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AyniShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, AyniHairline)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AyniPrimaryFixed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = AyniOnPrimaryContainer
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = Spacing.md)
            )
        }
    }
}
