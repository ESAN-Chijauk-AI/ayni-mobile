package com.ayni.mobile.ui.medical

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ayni.mobile.ui.components.BottomNavClearance
import com.ayni.mobile.ui.components.InjuryChip
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.theme.AyniHairline
import com.ayni.mobile.ui.theme.AyniInputBackground
import com.ayni.mobile.ui.theme.AyniOnPrimaryContainer
import com.ayni.mobile.ui.theme.AyniOutlineVariant
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniPrimaryFixed
import com.ayni.mobile.ui.theme.AyniSecondaryContainer
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing

/**
 * F3: input rápido de lesión. Chips de lesiones comunes primero (preferidas sobre
 * teclear, §3/F3) en una tarjeta propia; describir a mano y adjuntar foto son secciones
 * separadas y marcadas "Opcional" para que se lean como contexto extra para Gemma, no
 * como campos obligatorios.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalInputScreen(
    parentEntry: NavBackStackEntry,
    onResultReady: () -> Unit
) {
    val viewModel: MedicalViewModel = hiltViewModel(parentEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedImage by viewModel.selectedImage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is MedicalUiState.Result) onResultReady()
    }

    val input = uiState as? MedicalUiState.Input
    val analyzing = uiState is MedicalUiState.Analyzing
    val errorMessage = (uiState as? MedicalUiState.Error)?.message

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            viewModel.onImageSelected(bytes)
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(BottomNavClearance)
        ) {
            MedicalHeader()

            SectionCard(text = stringResource(R.string.medical_input_chips_label)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    COMMON_INJURIES.forEach { label ->
                        InjuryChip(
                            label = label,
                            selected = input?.selectedChips?.contains(label) == true,
                            onClick = { viewModel.onToggleChip(label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            SectionCard(text = stringResource(R.string.medical_input_free_text_section_title), optional = true) {
                OutlinedTextField(
                    value = input?.freeText.orEmpty(),
                    onValueChange = viewModel::onFreeTextChange,
                    placeholder = { Text(stringResource(R.string.medical_input_free_text_label)) },
                    minLines = 2,
                    shape = AyniShapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = AyniInputBackground,
                        focusedContainerColor = AyniInputBackground,
                        unfocusedBorderColor = AyniOutlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            SectionCard(text = stringResource(R.string.medical_input_photo_section_title), optional = true) {
                Text(
                    text = stringResource(R.string.medical_input_photo_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
                PhotoAttachment(
                    imageBytes = selectedImage,
                    onAdd = { photoPickerLauncher.launch("image/*") },
                    onRemove = { viewModel.onImageSelected(null) }
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
            PrimaryActionButton(
                text = if (analyzing) {
                    stringResource(R.string.medical_input_analyzing)
                } else {
                    stringResource(R.string.medical_input_submit)
                },
                enabled = !analyzing && input != null &&
                    (input.selectedChips.isNotEmpty() || input.freeText.isNotBlank()),
                onClick = viewModel::onSubmit
            )
        }
    }
}

@Composable
private fun MedicalHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Spacing.lg)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AyniPrimaryFixed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.HealthAndSafety, contentDescription = null, tint = AyniPrimaryContainer)
        }
        Column(modifier = Modifier.padding(start = Spacing.md)) {
            Text(text = stringResource(R.string.medical_input_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.medical_input_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(
    text: String,
    optional: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AyniShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, AyniHairline)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
                if (optional) {
                    Surface(
                        modifier = Modifier.padding(start = Spacing.sm),
                        shape = AyniShapes.small,
                        color = AyniSecondaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.medical_input_optional_tag),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            content()
        }
    }
}

@Composable
private fun PhotoAttachment(
    imageBytes: ByteArray?,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    if (imageBytes == null) {
        Surface(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .border(BorderStroke(1.dp, AyniOutlineVariant), AyniShapes.medium),
            shape = AyniShapes.medium,
            color = AyniInputBackground
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.AddAPhoto, contentDescription = null, tint = AyniOnPrimaryContainer)
                Text(
                    text = stringResource(R.string.medical_input_photo_add),
                    style = MaterialTheme.typography.labelLarge,
                    color = AyniOnPrimaryContainer,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
        }
    } else {
        val bitmap = remember(imageBytes) {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.medical_input_photo_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(AyniShapes.medium)
                    .border(BorderStroke(0.5.dp, AyniHairline), AyniShapes.medium)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .padding(Spacing.sm)
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.medical_input_photo_remove),
                    tint = Color.White
                )
            }
        }
    }
}
