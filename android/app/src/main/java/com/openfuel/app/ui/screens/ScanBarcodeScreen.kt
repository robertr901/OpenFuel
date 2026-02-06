package com.openfuel.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.ui.components.MealTypeDropdown
import com.openfuel.app.ui.components.UnitDropdown
import com.openfuel.app.ui.theme.Dimens
import com.openfuel.app.ui.util.formatCalories
import com.openfuel.app.ui.util.formatMacro
import com.openfuel.app.ui.util.parseDecimalInput
import com.openfuel.app.viewmodel.ScanBarcodeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBarcodeScreen(
    viewModel: ScanBarcodeViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.m)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.m),
        ) {
            if (!hasCameraPermission) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimens.m),
                        verticalArrangement = Arrangement.spacedBy(Dimens.s),
                    ) {
                        Text(
                            text = "Camera access is required to scan barcodes.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        ) {
                            Text("Grant camera permission")
                        }
                    }
                }
            } else {
                BarcodeCameraPreview(
                    isScanningEnabled = !uiState.isLookingUp && uiState.previewFood == null,
                    onBarcodeScanned = viewModel::onBarcodeDetected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                )
            }

            if (uiState.isLookingUp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
                Text(
                    text = "Looking up barcode...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (uiState.errorMessage != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimens.m),
                        verticalArrangement = Arrangement.spacedBy(Dimens.s),
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                            Button(onClick = { viewModel.retryLookup() }) {
                                Text("Retry")
                            }
                            Button(onClick = { viewModel.clearPreviewAndResume() }) {
                                Text("Scan again")
                            }
                        }
                    }
                }
            }

            if (uiState.previewFood != null) {
                ScannedFoodPreviewCard(
                    food = uiState.previewFood!!,
                    onSave = { viewModel.savePreviewFood() },
                    onSaveAndLog = { quantity, unit, mealType ->
                        viewModel.saveAndLogPreviewFood(
                            quantity = quantity,
                            unit = unit,
                            mealType = mealType,
                        )
                    },
                    onScanAnother = { viewModel.clearPreviewAndResume() },
                )
            }
        }
    }
}

@Composable
private fun BarcodeCameraPreview(
    isScanningEnabled: Boolean,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE,
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(lifecycleOwner, isScanningEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraSetup = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(mainExecutor) { imageProxy ->
                        if (!isScanningEnabled) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        processImageProxy(
                            imageProxy = imageProxy,
                            onBarcodeScanned = onBarcodeScanned,
                            scanner = scanner,
                        )
                    }
                }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
            )
        }
        cameraProviderFuture.addListener(cameraSetup, mainExecutor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
            scanner.close()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeScanned: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees,
    )

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            val rawValue = barcodes.firstNotNullOfOrNull { barcode ->
                barcode.rawValue?.trim()?.takeIf { it.isNotBlank() }
            }
            if (rawValue != null) {
                onBarcodeScanned(rawValue)
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

@Composable
private fun ScannedFoodPreviewCard(
    food: RemoteFoodCandidate,
    onSave: () -> Unit,
    onSaveAndLog: (Double, FoodUnit, MealType) -> Unit,
    onScanAnother: () -> Unit,
) {
    var quantityInput by rememberSaveable(food.sourceId) { mutableStateOf("1") }
    var selectedUnit by rememberSaveable(food.sourceId) { mutableStateOf(FoodUnit.SERVING) }
    var selectedMealType by rememberSaveable(food.sourceId) { mutableStateOf(MealType.BREAKFAST) }
    var quantityError by rememberSaveable(food.sourceId) { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.m),
            verticalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (!food.brand.isNullOrBlank()) {
                Text(
                    text = food.brand.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "${formatCalories(food.caloriesKcalPer100g ?: 0.0)} kcal · ${formatMacro(food.proteinGPer100g ?: 0.0)}p ${formatMacro(food.carbsGPer100g ?: 0.0)}c ${formatMacro(food.fatGPer100g ?: 0.0)}f per 100g",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!food.servingSize.isNullOrBlank()) {
                Text(
                    text = "Serving: ${food.servingSize}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = quantityInput,
                onValueChange = {
                    quantityInput = it
                    quantityError = null
                },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = quantityError != null,
                supportingText = {
                    if (quantityError != null) {
                        Text(quantityError ?: "")
                    }
                },
            )
            UnitDropdown(
                selected = selectedUnit,
                onSelected = { selectedUnit = it },
                modifier = Modifier.fillMaxWidth(),
            )
            MealTypeDropdown(
                selected = selectedMealType,
                onSelected = { selectedMealType = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
                Button(
                    onClick = {
                        val quantity = parseDecimalInput(quantityInput)
                        if (quantity == null || quantity <= 0.0) {
                            quantityError = "Enter a valid quantity."
                        } else {
                            onSaveAndLog(quantity, selectedUnit, selectedMealType)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save and Log")
                }
            }
            Button(
                onClick = onScanAnother,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan another")
            }
        }
    }
}
