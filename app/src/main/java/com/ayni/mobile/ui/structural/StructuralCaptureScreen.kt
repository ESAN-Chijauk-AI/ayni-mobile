package com.ayni.mobile.ui.structural

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.SensorSignatureReadout
import com.ayni.mobile.ui.theme.AyniOutlineVariant
import com.ayni.mobile.ui.theme.AyniPrimaryContainer
import com.ayni.mobile.ui.theme.AyniShapes
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * F2 (captura), restyle stitch_remix_of_ayni_mobile_emergency_response/c_mara_de_inspecci_n:
 * overlay oscuro tipo instrumento sobre el preview real de CameraX, retícula de encuadre,
 * badge "AI SENSOR RUNNING", panel inferior frosted con el readout de sensor real (firma
 * del spec original) y el control segmentado de severidad — este último es **solo visual**
 * (no hay clasificación de severidad propia todavía; el veredicto real lo da Gemma después
 * de analizar). Al llegar a StructuralUiState.Result, navega al Reporte.
 */
@Composable
fun StructuralCaptureScreen(
    parentEntry: NavBackStackEntry,
    onResultReady: () -> Unit,
    onClose: () -> Unit = {},
    onSensorStatusClick: () -> Unit = {},
    onMonitoringClick: () -> Unit = {}
) {
    val viewModel: StructuralViewModel = hiltViewModel(parentEntry)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sensorConnected by viewModel.sensorConnected.collectAsStateWithLifecycle()
    val magnitudes by viewModel.magnitudes.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is StructuralUiState.Result) onResultReady()
    }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreviewWithCapture(
                onCaptured = { bytes -> viewModel.onImageCaptured(bytes) },
                enabled = uiState is StructuralUiState.Capturing,
                sensorConnected = sensorConnected,
                magnitudes = magnitudes,
                onClose = onClose,
                onSensorStatusClick = onSensorStatusClick,
                onMonitoringClick = onMonitoringClick
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.structural_camera_permission_needed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }

        if (uiState is StructuralUiState.Analyzing) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AyniPrimaryContainer)
                    Text(
                        text = stringResource(R.string.structural_analyzing),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }
            }
        }

        val errorState = uiState as? StructuralUiState.Error
        if (errorState != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = Spacing.md, end = Spacing.md),
                shape = AyniShapes.medium,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(text = errorState.message, modifier = Modifier.padding(Spacing.md))
            }
        }
    }
}

@Composable
private fun CameraPreviewWithCapture(
    onCaptured: (ByteArray) -> Unit,
    enabled: Boolean,
    sensorConnected: Boolean,
    magnitudes: List<Float>,
    onClose: () -> Unit,
    onSensorStatusClick: () -> Unit,
    onMonitoringClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            // Captura a ~1280px de lado largo en vez de la resolución nativa del sensor
            // (a veces 12MP+): menos memoria/CPU en el decode posterior y en el resize
            // a 512-768px que hace GemmaAiRepository antes de mandarla al modelo.
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                )
                .build()
            val capture = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
            }
            imageCapture = capture
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Degradado sutil para que el chrome se lea sobre cualquier foto de fondo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.5f),
                        0.25f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f)
                    )
                )
        )

        // Top bar: cerrar / badge AI SENSOR RUNNING / flash (visual).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(icon = Icons.Filled.Close, contentDescription = stringResource(R.string.action_back), onClick = onClose)
            Surface(
                shape = AyniShapes.large,
                color = Color.Black.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Text(
                    text = stringResource(R.string.structural_ai_sensor_running),
                    style = MaterialTheme.typography.labelMedium,
                    color = AyniPrimaryContainer,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                RoundIconButton(icon = Icons.Filled.Sensors, contentDescription = stringResource(R.string.nav_monitoring), onClick = onMonitoringClick)
                RoundIconButton(icon = Icons.Filled.FlashOn, contentDescription = null, onClick = {})
            }
        }

        // Retícula central (guía de encuadre, decorativa — la foto real se manda entera).
        Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(220.dp)) {
                ReticleCorner(Alignment.TopStart)
                ReticleCorner(Alignment.TopEnd)
                ReticleCorner(Alignment.BottomStart)
                ReticleCorner(Alignment.BottomEnd)
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 160.dp),
            shape = AyniShapes.small,
            color = Color.Black.copy(alpha = 0.5f)
        ) {
            Text(
                text = stringResource(R.string.structural_align_hint),
                style = MaterialTheme.typography.labelSmall,
                color = AyniPrimaryContainer,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }

        // Panel inferior frosted: readout real de sensor + control de severidad (visual) + captura.
        val sensorStatusDescription = stringResource(R.string.structural_sensor_status_link)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSensorStatusClick)
                    .semantics { contentDescription = sensorStatusDescription }
            ) {
                SensorSignatureReadout(
                    magnitudes = magnitudes,
                    connected = sensorConnected,
                    isSimulated = true
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            SeveritySegmentedControlPlaceholder()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(56.dp))
                CaptureRingButton(
                    enabled = enabled && imageCapture != null,
                    onClick = {
                        val capture = imageCapture ?: return@CaptureRingButton
                        val tempFile = File(context.cacheDir, "ayni_capture_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    coroutineScope.launch {
                                        val bytes = withContext(Dispatchers.IO) {
                                            runCatching { tempFile.readBytes() }.getOrNull().also { tempFile.delete() }
                                        }
                                        bytes?.let(onCaptured)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // Se ignora a propósito: el usuario puede volver a tocar
                                    // "Capturar" (enabled sigue en true, no quedó bloqueado).
                                    tempFile.delete()
                                }
                            }
                        )
                    }
                )
                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
        }
    }
}

@Composable
private fun ReticleCorner(alignment: Alignment) {
    // Esquinas simples en ámbar (color de marca), estilo mira de instrumento.
    Box(
        modifier = Modifier
            .size(220.dp),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.Transparent)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                drawLine(AyniPrimaryContainer, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), stroke)
                drawLine(AyniPrimaryContainer, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), stroke)
            }
        }
    }
}

/** Visual-only: no hay clasificación de severidad propia todavía (§ nota de alcance). */
@Composable
private fun SeveritySegmentedControlPlaceholder() {
    var selected by remember { mutableStateOf(1) }
    val labels = listOf(
        stringResource(R.string.structural_severity_mild),
        stringResource(R.string.structural_severity_moderate),
        stringResource(R.string.structural_severity_high)
    )
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, AyniOutlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Surface(
                    onClick = { selected = index },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = if (isSelected) AyniPrimaryContainer else Color.Transparent
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureRingButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(6.dp, AyniPrimaryContainer),
        modifier = run {
            val description = stringResource(R.string.structural_capture_action)
            Modifier
                .size(80.dp)
                .semantics { contentDescription = description }
        }
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.25f), CircleShape)
        )
    }
}
