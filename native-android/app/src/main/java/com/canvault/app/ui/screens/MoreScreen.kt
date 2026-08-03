package com.canvault.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.BuildConfig
import com.canvault.app.data.CanItem
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.brandName
import com.canvault.app.data.lineName
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import java.io.File

@Composable
fun MoreScreen(
    repository: InventoryRepository,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenMarket: () -> Unit,
) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sounds = LocalCanVaultSounds.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    sounds.play(UiSoundEffect.NAVIGATION)
                    onBack()
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück zu Projekten")
            }
            Column(Modifier.padding(start = 8.dp)) {
                Text("Mehr", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Markt, Export und App-Informationen", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = {
                sounds.play(UiSoundEffect.STANDARD)
                onOpenMarket()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Rounded.Storefront, contentDescription = null)
            Text("Can-Markt öffnen", Modifier.padding(start = 8.dp))
        }

        Button(
            onClick = {
                sounds.play(UiSoundEffect.SUCCESS)
                shareCsv(context, snapshot.cans)
            },
            enabled = snapshot.cans.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Text("Gesamte History als CSV teilen", Modifier.padding(start = 8.dp))
        }

        Text("Über CANVAULT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            InfoRow(Icons.Rounded.CloudOff, "Offline-first", "Inventar und Fotos bleiben lokal verfügbar.")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            InfoRow(Icons.Rounded.Security, "Private Kamera", "Codes werden direkt auf dem Handy verarbeitet.")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            InfoRow(Icons.Rounded.Info, "Native Android-App", "Version ${BuildConfig.VERSION_NAME} · Paket com.canvault.app")
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun shareCsv(context: Context, cans: List<CanItem>) {
    val exportDirectory = File(context.cacheDir, "exports").apply { mkdirs() }
    val exportFile = File(exportDirectory, "canvault-history.csv")
    val header = "id;marke;linie;farbe;farbcode;hex;volumen_ml;fuellstand_prozent;status;preis_cent\n"
    val rows = cans.joinToString("\n") { can ->
        listOf(
            can.id,
            brandName(can.brandId),
            lineName(can.canLineId),
            can.colorName,
            can.colorCode.orEmpty(),
            can.customHex.orEmpty(),
            can.volumeMl?.toString().orEmpty(),
            can.fillPercent?.toString().orEmpty(),
            can.status.name,
            can.purchasePriceCents?.toString().orEmpty(),
        ).joinToString(";") { value -> "\"${value.replace("\"", "\"\"")}\"" }
    }
    exportFile.writeText(header + rows, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", exportFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Inventar teilen"))
}
