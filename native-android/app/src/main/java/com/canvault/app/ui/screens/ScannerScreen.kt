package com.canvault.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.data.BarcodeStabilizer
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.ProductBarcodeResolution
import com.canvault.app.data.QrCodec
import com.canvault.app.data.ScanInterpretation
import com.canvault.app.data.SharedCatalogRepository
import com.canvault.app.data.brandName
import com.canvault.app.data.lineName
import com.canvault.app.data.resolveProductBarcode
import com.canvault.app.data.toProductBarcodePrefill
import com.canvault.app.ui.theme.CanVaultColors
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import com.canvault.app.ui.sound.soundClick
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

private enum class ScannerMode(val label: String) {
    PRODUCT_BARCODE("Produkt-Barcode"),
    CANVAULT_QR("CANVAULT QR"),
}

private const val ScannerLogTag = "CANVAULT_SCANNER"

private data class DetectedScan(
    val headline: String,
    val description: String,
    val rawValue: String,
    val format: String,
    val actionLabel: String,
    val prefill: ScanPrefill,
    val autoContinue: Boolean = false,
)

private sealed interface DecodedScanOutcome {
    data class Success(val scan: DetectedScan) : DecodedScanOutcome
    data class Error(val message: String) : DecodedScanOutcome
}

private fun interpretDecodedScan(
    mode: ScannerMode,
    snapshot: com.canvault.app.data.InventorySnapshot,
    verifiedCatalog: com.canvault.app.data.VerifiedCatalogSnapshot,
    raw: String,
    format: String,
): DecodedScanOutcome {
    if (mode == ScannerMode.PRODUCT_BARCODE) {
        return when (val result = resolveProductBarcode(snapshot, raw, format, verifiedCatalog)) {
            is ProductBarcodeResolution.Verified -> {
                val product = result.product
                val automatic = result.toProductBarcodePrefill()!!
                DecodedScanOutcome.Success(
                    DetectedScan(
                        headline = "Verifiziert erkannt",
                        description = "${brandName(product.brandId)} · ${lineName(product.lineId)} · ${product.colorName}",
                        rawValue = result.scannedValue,
                        format = result.format,
                        actionLabel = "Produkt übernehmen",
                        prefill = automatic.toScanPrefill(),
                        autoContinue = true,
                    ),
                )
            }

            is ProductBarcodeResolution.Known -> {
                val can = result.can
                val automatic = result.toProductBarcodePrefill()!!
                DecodedScanOutcome.Success(
                    DetectedScan(
                        headline = "Produkt erkannt",
                        description = "${brandName(can.brandId)} · ${lineName(can.canLineId)} · ${can.colorName}",
                        rawValue = result.scannedValue,
                        format = result.format,
                        actionLabel = "Produkt übernehmen",
                        prefill = automatic.toScanPrefill(),
                        autoContinue = true,
                    ),
                )
            }

            is ProductBarcodeResolution.New -> DecodedScanOutcome.Success(
                DetectedScan(
                    headline = "Neuer Produkt-Barcode",
                    description = "Produkt einmal ergänzen; danach erkennt CANVAULT diesen Barcode automatisch.",
                    rawValue = result.scannedValue,
                    format = result.format,
                    actionLabel = "Dose anlegen",
                    prefill = ScanPrefill(
                        externalBarcode = result.scannedValue,
                        message = "Dieser Barcode enthält nur eine Produkt-ID und ist noch nicht zugeordnet. Produktdaten bitte einmal ergänzen.",
                    ),
                ),
            )

            is ProductBarcodeResolution.Invalid -> DecodedScanOutcome.Error(result.message)
        }
    }

    return when (val interpretation = QrCodec.interpret(raw, format)) {
        is ScanInterpretation.CatalogMatch -> {
            val payload = interpretation.payload
            DecodedScanOutcome.Success(
                DetectedScan(
                    headline = "CANVAULT QR erkannt",
                    description = "${brandName(payload.brandId)} · ${lineName(payload.canLineId)} · ${payload.colorName}",
                    rawValue = interpretation.rawValue,
                    format = "QR",
                    actionLabel = "Produkt übernehmen",
                    prefill = ScanPrefill(
                        brandId = payload.brandId,
                        lineId = payload.canLineId,
                        colorName = payload.colorName,
                        colorCode = payload.colorCode,
                        customHex = payload.customHex,
                        message = "CANVAULT QR erkannt – Produktdaten wurden übernommen.",
                    ),
                    autoContinue = true,
                ),
            )
        }

        is ScanInterpretation.ExternalCode -> DecodedScanOutcome.Error(
            "Das ist kein CANVAULT QR. Für EAN, UPC und andere Strichcodes bitte ‚Produkt-Barcode‘ wählen.",
        )
        is ScanInterpretation.Invalid -> DecodedScanOutcome.Error(interpretation.message)
    }
}

private fun com.canvault.app.data.ProductBarcodePrefill.toScanPrefill() = ScanPrefill(
    brandId = brandId,
    lineId = lineId,
    colorName = colorName,
    colorCode = colorCode,
    customHex = customHex,
    volumeMl = volumeMl,
    purchasePriceCents = purchasePriceCents,
    externalBarcode = externalBarcode,
    message = sourceMessage,
)

@Composable
fun ScannerScreen(
    repository: InventoryRepository,
    sharedCatalogRepository: SharedCatalogRepository,
    onBack: () -> Unit,
    onResult: (ScanPrefill) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val verifiedCatalog by sharedCatalogRepository.snapshot.collectAsStateWithLifecycle()
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var mode by remember { mutableStateOf(ScannerMode.PRODUCT_BARCODE) }
    var torchEnabled by remember { mutableStateOf(false) }
    var detectedScan by remember { mutableStateOf<DetectedScan?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanSession by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    fun restartScanning() {
        detectedScan = null
        errorMessage = null
        scanSession += 1
    }

    LaunchedEffect(mode) {
        restartScanning()
    }
    LaunchedEffect(detectedScan) {
        val scan = detectedScan ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (scan.autoContinue) {
            delay(350)
            if (detectedScan == scan) onResult(scan.prefill)
        }
    }

    fun handleDecodedCode(raw: String, format: String) {
        Log.i(ScannerLogTag, "Decoded $format barcode (${raw.length} characters)")
        when (val outcome = interpretDecodedScan(mode, snapshot, verifiedCatalog, raw, format)) {
            is DecodedScanOutcome.Success -> detectedScan = outcome.scan
            is DecodedScanOutcome.Error -> errorMessage = outcome.message
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permissionGranted) {
            CameraBarcodePreview(
                key = scanSession,
                mode = mode,
                torchEnabled = torchEnabled,
                onBarcode = ::handleDecodedCode,
                onError = {
                    errorMessage = "Die Kamera konnte den Scanner nicht starten. Tippe auf ‚Weiter scannen‘ oder prüfe die Kameraberechtigung."
                },
            )
            ScannerGuide(
                mode = mode,
                paused = detectedScan != null || errorMessage != null,
            )
        } else {
            CameraPermissionPrompt(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
        }

        ScannerTopBar(
            permissionGranted = permissionGranted,
            torchEnabled = torchEnabled,
            onBack = onBack,
            onToggleTorch = { torchEnabled = !torchEnabled },
        )

        if (permissionGranted) {
            ScannerModeSelector(
                mode = mode,
                onModeChange = { selected ->
                    if (mode != selected) mode = selected
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 68.dp, end = 16.dp),
            )
        }

        detectedScan?.let { result ->
            ScanResultCard(
                result = result,
                onConfirm = { onResult(result.prefill) },
                onRetry = ::restartScanning,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(
                        onClick = soundClick(UiSoundEffect.SCAN, ::restartScanning),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("Weiter scannen")
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    onRequest: () -> Unit,
) {
    val requestClick = soundClick(onClick = onRequest)
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = CanVaultColors.Mint,
            modifier = Modifier.size(52.dp),
        )
        Text(
            "Kamerazugriff erforderlich",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Text(
            "Barcodes werden vollständig auf diesem Gerät gelesen. Kamerabilder werden nicht hochgeladen.",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            color = Color.White.copy(alpha = 0.75f),
        )
        Button(onClick = requestClick, modifier = Modifier.height(52.dp)) {
            Text("Kamera erlauben")
        }
    }
}

@Composable
private fun ScannerTopBar(
    permissionGranted: Boolean,
    torchEnabled: Boolean,
    onBack: () -> Unit,
    onToggleTorch: () -> Unit,
) {
    val backClick = soundClick(onClick = onBack)
    val torchClick = soundClick(onClick = onToggleTorch)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = backClick,
            modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.62f)),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück", tint = Color.White)
        }
        if (permissionGranted) {
            IconButton(
                onClick = torchClick,
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.62f)),
            ) {
                Icon(
                    if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = if (torchEnabled) "Licht ausschalten" else "Licht einschalten",
                    tint = if (torchEnabled) CanVaultColors.Warning else Color.White,
                )
            }
        }
    }
}

@Composable
private fun ScannerModeSelector(
    mode: ScannerMode,
    onModeChange: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sounds = LocalCanVaultSounds.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.68f),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ScannerMode.entries.forEach { option ->
                val selected = option == mode
                Button(
                    onClick = {
                        if (!selected) {
                            sounds.play(UiSoundEffect.STANDARD)
                            onModeChange(option)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) CanVaultColors.Mint else Color.Transparent,
                        contentColor = if (selected) Color.Black else Color.White,
                    ),
                ) {
                    Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ScannerGuide(
    mode: ScannerMode,
    paused: Boolean,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val transition = rememberInfiniteTransition(label = "scanner-line")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion || paused) 0.5f else 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "scanner-progress",
    )
    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (mode == ScannerMode.PRODUCT_BARCODE) "Strichcode vollständig in den Rahmen halten" else "CANVAULT QR in den Rahmen halten",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        BoxWithConstraints(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(if (mode == ScannerMode.PRODUCT_BARCODE) 0.86f else 0.68f)
                .height(if (mode == ScannerMode.PRODUCT_BARCODE) 156.dp else 264.dp)
                .border(2.dp, CanVaultColors.Mint, RoundedCornerShape(24.dp)),
        ) {
            val travelPx = with(density) { (maxHeight - 4.dp).toPx() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .graphicsLayer { translationY = travelPx * progress }
                    .background(CanVaultColors.Mint)
                    .size(height = 3.dp, width = maxWidth),
            )
        }
        Text(
            if (mode == ScannerMode.PRODUCT_BARCODE) {
                "EAN-13/8 · UPC-A/E · Code 128/39/93 · ITF · Codabar"
            } else {
                "Nur von CANVAULT erzeugte QR-Codes"
            },
            modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ScanResultCard(
    result: DetectedScan,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryClick = soundClick(UiSoundEffect.SCAN, onRetry)
    Card(
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CanVaultColors.Mint)
                Text(
                    result.headline,
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(result.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(result.format, style = MaterialTheme.typography.labelMedium)
                    Text(
                        result.rawValue,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (result.autoContinue) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                        )
                        Text(
                            "Daten werden automatisch eingetragen …",
                            modifier = Modifier.padding(start = 10.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text(result.actionLabel)
                }
            }
            OutlinedButton(onClick = retryClick, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Erneut scannen")
            }
        }
    }
}

@Composable
private fun CameraBarcodePreview(
    key: Int,
    mode: ScannerMode,
    torchEnabled: Boolean,
    onBarcode: (String, String) -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val controller = remember(key, mode) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }
    val scanner = remember(key, mode) { BarcodeScanning.getClient(scannerOptions(mode)) }
    val handled = remember(key, mode) { AtomicBoolean(false) }
    val reportedFailure = remember(key, mode) { AtomicBoolean(false) }
    val stabilizer = remember(key, mode) { BarcodeStabilizer(requiredHits = 1, maxGapMs = 900) }

    AndroidView(
        factory = { previewContext ->
            PreviewView(previewContext).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        update = { it.controller = controller },
        modifier = Modifier.fillMaxSize(),
    )

    LaunchedEffect(torchEnabled, controller) {
        runCatching { controller.enableTorch(torchEnabled) }
            .onFailure { Log.w(ScannerLogTag, "Torch change failed", it) }
    }

    DisposableEffect(controller, scanner, lifecycleOwner) {
        val analyzer = MlKitAnalyzer(
            listOf(scanner),
            CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
            mainExecutor,
        ) { result ->
            val failure = result.getThrowable(scanner)
            if (failure != null && reportedFailure.compareAndSet(false, true)) {
                Log.e(ScannerLogTag, "Embedded ML Kit analyzer failed", failure)
                onError()
            }

            val barcode = result.getValue(scanner)
                .orEmpty()
                .asSequence()
                .filter { !it.rawValue.isNullOrBlank() }
                .maxByOrNull { candidate ->
                    candidate.boundingBox?.let { it.width() * it.height() } ?: 0
                }
            if (barcode != null) {
                val stable = stabilizer.offer(
                    rawValue = barcode.rawValue.orEmpty(),
                    format = barcode.formatName(),
                    nowMs = android.os.SystemClock.elapsedRealtime(),
                )
                if (stable != null && handled.compareAndSet(false, true)) {
                    Log.i(ScannerLogTag, "Embedded scanner decoded ${stable.format}")
                    onBarcode(stable.rawValue, stable.format)
                }
            }
        }

        controller.setImageAnalysisAnalyzer(mainExecutor, analyzer)
        runCatching {
            controller.bindToLifecycle(lifecycleOwner)
            Log.i(ScannerLogTag, "LifecycleCameraController bound for ${mode.name}")
        }.onFailure { error ->
            Log.e(ScannerLogTag, "Camera controller binding failed", error)
            if (reportedFailure.compareAndSet(false, true)) onError()
        }

        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            scanner.close()
        }
    }
}

private fun scannerOptions(mode: ScannerMode): BarcodeScannerOptions =
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            if (mode == ScannerMode.CANVAULT_QR) Barcode.FORMAT_QR_CODE else Barcode.FORMAT_EAN_13,
            *if (mode == ScannerMode.CANVAULT_QR) {
                intArrayOf()
            } else {
                intArrayOf(
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_CODABAR,
                )
            },
        )
        .build()

private fun Barcode.formatName(): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    Barcode.FORMAT_CODE_128 -> "Code 128"
    Barcode.FORMAT_CODE_39 -> "Code 39"
    Barcode.FORMAT_CODE_93 -> "Code 93"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_CODABAR -> "Codabar"
    else -> "Barcode"
}
