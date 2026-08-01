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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.ayni.mobile.R
import com.ayni.mobile.ui.components.PrimaryActionButton
import com.ayni.mobile.ui.components.SensorSignatureReadout
import com.ayni.mobile.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * F2 (captura): CameraX preview + botón de captura + estado "analizando" inline
 * superpuesto (evita una ruta separada, cumple "arranque a veredicto en <=3 taps").
 * Al llegar a StructuralUiState.Result, navega a la pantalla de resultado.
 *
 * @param parentEntry backstack entry de STRUCTURAL_GRAPH: el ViewModel se scopea a él
 * para que sobreviva la navegación hacia StructuralResultScreen (mismo flujo, un solo
 * análisis en curso).
 */
@Composable
fun StructuralCaptureScreen(
    parentEntry: NavBackStackEntry,
    onResultReady: () -> Unit
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreviewWithCapture(
                onCaptured = { bytes -> viewModel.onImageCaptured(bytes) },
                enabled = uiState is StructuralUiState.Capturing
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(Spacing.md)
        ) {
            SensorSignatureReadout(
                magnitudes = magnitudes,
                connected = sensorConnected,
                isSimulated = true
            )
        }

        if (uiState is StructuralUiState.Analyzing) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.structural_analyzing),
                        style = MaterialTheme.typography.titleLarge,
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
                    .padding(Spacing.md),
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
    enabled: Boolean
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
            // a 512-768px que hace GemmaAiRepository antes de mandarla al modelo. El
            // freeze reportado en dispositivos reales viene de sumar esto a una
            // inferencia CPU-only ya pesada — esto reduce la parte que sí controlamos.
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryActionButton(
                text = stringResource(R.string.structural_capture_action),
                enabled = enabled && imageCapture != null,
                contentDescription = stringResource(R.string.structural_capture_action_description),
                onClick = {
                    val capture = imageCapture ?: return@PrimaryActionButton
                    // Captura a un archivo temporal en cache (patrón estándar de CameraX,
                    // ver takePicture(OutputFileOptions, Executor, OnImageSavedCallback)):
                    // evita la conversión manual ImageProxy->Bitmap y sus incompatibilidades
                    // de API entre versiones de camera-core.
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
        }
    }
}
