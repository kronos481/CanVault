package com.canvault.app.ui.components

import android.graphics.Color.parseColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.canvault.app.R
import com.canvault.app.data.CanItem
import com.canvault.app.data.CanStatus
import com.canvault.app.data.brandName
import com.canvault.app.data.lineName
import com.canvault.app.data.resolveCanColorHex
import com.canvault.app.ui.assets.brandLogoRes
import com.canvault.app.ui.assets.canArtworkRes
import com.canvault.app.ui.sound.soundClick
import com.canvault.app.ui.theme.CanVaultColors

fun CanStatus.label(): String = when (this) {
    CanStatus.IN_STOCK -> "Auf Lager"
    CanStatus.OPENED -> "Geöffnet"
    CanStatus.RESERVED -> "Reserviert"
    CanStatus.EMPTY -> "Leer"
    CanStatus.COLLECTION -> "Sammlung"
    CanStatus.ARCHIVED -> "Archiviert"
}

fun safeCanColor(hex: String?): Color = try {
    if (hex == null) Color(0xFF7B8490) else Color(parseColor(hex))
} catch (_: IllegalArgumentException) {
    Color(0xFF7B8490)
}

fun safeCanColor(can: CanItem): Color = safeCanColor(resolveCanColorHex(can))

@Composable
fun CanCard(
    can: CanItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent by animateColorAsState(safeCanColor(can), label = "can-color")
    val audibleClick = soundClick(onClick = onClick)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = audibleClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxWidth().height(8.dp).background(accent))
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CanArtwork(can = can, modifier = Modifier.size(width = 88.dp, height = 128.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BrandLogo(
                    brandId = can.brandId,
                    modifier = Modifier.height(26.dp).widthIn(max = 120.dp),
                )
                Text(
                    text = lineName(can.canLineId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(can.colorName, can.colorCode).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(can.status)
                    Text(
                        text = can.fillPercent?.let { "$it %" } ?: "Unbekannt",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                LinearProgressIndicator(
                    progress = { (can.fillPercent ?: 0) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .semantics { contentDescription = "Füllstand ${can.fillPercent ?: 0} Prozent" },
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CanArtwork(can: CanItem, modifier: Modifier = Modifier) {
    val fallback = if (can.id.hashCode() % 2 == 0) R.drawable.can_mint else R.drawable.can_violet
    val catalogArtwork = canArtworkRes(can.canLineId)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (can.photoPath != null) {
            AsyncImage(
                model = can.photoPath,
                contentDescription = "Foto von ${can.colorName}",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (catalogArtwork != null) {
            Image(
                painter = painterResource(catalogArtwork),
                contentDescription = "${brandName(can.brandId)} ${lineName(can.canLineId)} Produktbild",
                modifier = Modifier.matchParentSize().padding(4.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Image(
                painter = painterResource(fallback),
                contentDescription = "Generische Dosenillustration",
                modifier = Modifier.matchParentSize().padding(6.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun BrandLogo(
    brandId: String,
    modifier: Modifier = Modifier,
) {
    val resource = brandLogoRes(brandId)
    if (resource != null) {
        Image(
            painter = painterResource(resource),
            contentDescription = "${brandName(brandId)} Logo",
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CanVaultColors.RaisedSurface)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            alignment = Alignment.CenterStart,
            contentScale = ContentScale.Fit,
        )
    } else {
        Text(
            text = brandName(brandId),
            modifier = modifier,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun StatusPill(status: CanStatus) {
    val tint = when (status) {
        CanStatus.EMPTY, CanStatus.ARCHIVED -> MaterialTheme.colorScheme.onSurfaceVariant
        CanStatus.OPENED -> CanVaultColors.Warning
        else -> MaterialTheme.colorScheme.primary
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(status.label(), fontSize = 11.sp) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = tint.copy(alpha = 0.14f),
            disabledLabelColor = tint,
        ),
        border = null,
    )
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val scale by animateFloatAsState(1f, label = "stat-enter")
    Card(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
