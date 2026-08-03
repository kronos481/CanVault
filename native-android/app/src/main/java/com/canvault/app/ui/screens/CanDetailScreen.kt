package com.canvault.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.data.CanItem
import com.canvault.app.data.CanStatus
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.QrCodec
import com.canvault.app.data.lineName
import com.canvault.app.ui.components.CanArtwork
import com.canvault.app.ui.components.BrandLogo
import com.canvault.app.ui.components.label
import com.canvault.app.ui.components.safeCanColor
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import com.canvault.app.ui.sound.soundClick
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanDetailScreen(
    repository: InventoryRepository,
    canId: String,
    onBack: () -> Unit,
) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val can = snapshot.cans.firstOrNull { it.id == canId }
    val events = snapshot.events.filter { it.canId == canId }.sortedByDescending { it.occurredAt }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sounds = LocalCanVaultSounds.current
    val backClick = soundClick(onClick = onBack)
    var showArchiveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(can?.colorName ?: "Dose") },
                navigationIcon = {
                    IconButton(onClick = backClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(targetState = can, label = "can-detail") { selectedCan ->
            if (selectedCan == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Dose wurde nicht gefunden.")
                }
            } else {
                DetailContent(
                    can = selectedCan,
                    events = events.map { it.description to it.occurredAt },
                    contentPadding = padding,
                    onFillChanged = { percent ->
                        sounds.play(UiSoundEffect.STANDARD)
                        scope.launch { repository.updateFill(selectedCan.id, percent) }
                    },
                    onStatusChanged = { status ->
                        sounds.play(UiSoundEffect.STANDARD)
                        scope.launch { repository.updateStatus(selectedCan.id, status) }
                    },
                    onArchive = {
                        sounds.play(UiSoundEffect.STANDARD)
                        showArchiveDialog = true
                    },
                    onShareQr = {
                        sounds.play(UiSoundEffect.STANDARD)
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "CANVAULT – ${selectedCan.colorName}")
                            putExtra(Intent.EXTRA_TEXT, QrCodec.encode(selectedCan))
                        }
                        context.startActivity(Intent.createChooser(share, "CANVAULT-Code teilen"))
                    },
                )
            }
        }
    }

    if (showArchiveDialog && can != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Dose archivieren?") },
            text = { Text("Die Dose bleibt gespeichert und kann im Archiv jederzeit wiederhergestellt werden.") },
            confirmButton = {
                Button(onClick = {
                    sounds.play(UiSoundEffect.ARCHIVE)
                    showArchiveDialog = false
                    scope.launch {
                        repository.archive(can.id)
                        onBack()
                    }
                }) { Text("Archivieren") }
            },
            dismissButton = {
                TextButton(onClick = {
                    sounds.play(UiSoundEffect.STANDARD)
                    showArchiveDialog = false
                }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun DetailContent(
    can: CanItem,
    events: List<Pair<String, Long>>,
    contentPadding: PaddingValues,
    onFillChanged: (Int) -> Unit,
    onStatusChanged: (CanStatus) -> Unit,
    onArchive: () -> Unit,
    onShareQr: () -> Unit,
) {
    var fill by remember(can.id, can.fillPercent) { mutableFloatStateOf((can.fillPercent ?: 0).toFloat()) }
    val qrBitmap = remember(can) { createQrBitmap(QrCodec.encode(can), 560) }
    val accent = safeCanColor(can)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(6.dp).background(accent))
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                CanArtwork(can, Modifier.size(width = 118.dp, height = 180.dp))
                Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BrandLogo(
                        brandId = can.brandId,
                        modifier = Modifier.height(34.dp).widthIn(max = 150.dp),
                    )
                    Text(lineName(can.canLineId), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(can.colorName, style = MaterialTheme.typography.titleMedium)
                    can.colorCode?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    can.volumeMl?.let { Text("$it ml", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }

        Text("Füllstand", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("${fill.toInt()} %", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
                Slider(
                    value = fill,
                    onValueChange = { fill = it },
                    onValueChangeFinished = { onFillChanged(fill.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                )
                Text("Geschätzter Wert – jederzeit anpassbar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text("Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CanStatus.entries.filter { it != CanStatus.ARCHIVED }.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { status ->
                        FilterChip(
                            selected = can.status == status,
                            onClick = { onStatusChanged(status) },
                            label = { Text(status.label()) },
                        )
                    }
                }
            }
        }

        Text("CANVAULT QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR-Code für ${can.colorName}",
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }
        OutlinedButton(onClick = onShareQr, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Rounded.Share, contentDescription = null)
            Text("Code teilen", Modifier.padding(start = 8.dp))
        }

        Text("Verlauf", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (events.isEmpty()) {
            Text("Noch keine Ereignisse", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            events.forEach { (description, timestamp) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(8.dp).background(accent, RoundedCornerShape(4.dp)))
                    Column(Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                        Text(description, fontWeight = FontWeight.Medium)
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (can.status != CanStatus.ARCHIVED) {
            OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Archive, contentDescription = null)
                Text("Dose archivieren", Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun createQrBitmap(content: String, size: Int): Bitmap {
    val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}
