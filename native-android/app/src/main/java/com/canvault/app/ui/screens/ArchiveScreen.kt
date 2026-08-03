package com.canvault.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.data.CanEvent
import com.canvault.app.data.CanItem
import com.canvault.app.data.CanStatus
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.StorageMonthStat
import com.canvault.app.data.StorageStats
import com.canvault.app.data.brandName
import com.canvault.app.data.calculateStorageStats
import com.canvault.app.data.lineName
import com.canvault.app.ui.components.CanCard
import com.canvault.app.ui.components.EmptyState
import com.canvault.app.ui.components.StatCard
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private enum class StorageSection {
    ARCHIVE,
    HISTORY,
}

@Composable
fun StorageScreen(
    repository: InventoryRepository,
    contentPadding: PaddingValues,
    onOpenCan: (String) -> Unit,
) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(StorageSection.ARCHIVE) }
    val archived = remember(snapshot.cans) {
        snapshot.cans.filter { it.status == CanStatus.ARCHIVED }.sortedByDescending { it.archivedAt ?: it.updatedAt }
    }
    val history = remember(snapshot.cans) { snapshot.cans.sortedByDescending(CanItem::acquiredAt) }
    val stats = remember(snapshot.cans) { calculateStorageStats(snapshot.cans) }
    val eventsByCan = remember(snapshot.events) { snapshot.events.groupBy(CanEvent::canId) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sounds = LocalCanVaultSounds.current
    var canPendingDeletion by remember { mutableStateOf<CanItem?>(null) }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Speicher", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Archiv und vollständige Dosen-History an einem Ort",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                StorageSectionSelector(
                    selected = section,
                    archiveCount = archived.size,
                    historyCount = history.size,
                    onSelected = { section = it },
                )
            }

            when (section) {
                StorageSection.ARCHIVE -> {
                    item {
                        SectionHeading(
                            title = "Archiv",
                            body = "Aus dem Bestand entfernt, aber sicher gespeichert und jederzeit wiederherstellbar.",
                        )
                    }
                    if (archived.isEmpty()) {
                        item {
                            EmptyState("Archiv ist leer", "Archivierte Dosen erscheinen hier und können wiederhergestellt werden.")
                        }
                    } else {
                        items(archived, key = { "archive-${it.id}" }) { can ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CanCard(can = can, onClick = { onOpenCan(can.id) })
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = {
                                            sounds.play(UiSoundEffect.RESTORE)
                                            scope.launch {
                                                repository.restore(can.id)
                                                val result = snackbar.showSnackbar(
                                                    message = "Dose wiederhergestellt",
                                                    actionLabel = "Rückgängig",
                                                    withDismissAction = true,
                                                )
                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    sounds.play(UiSoundEffect.ARCHIVE)
                                                    repository.archive(can.id)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1.35f).height(52.dp),
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null)
                                        Text("Wiederherstellen", Modifier.padding(start = 8.dp))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            sounds.play(UiSoundEffect.STANDARD)
                                            canPendingDeletion = can
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.62f),
                                        ),
                                    ) {
                                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                                        Text("Löschen", Modifier.padding(start = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                StorageSection.HISTORY -> {
                    item { HistoryStats(stats) }
                    item { MonthlyActivityCard(stats.monthlyActivity) }
                    item {
                        SectionHeading(
                            title = "Alle jemals erfassten Dosen",
                            body = "${history.size} Dosen · Archivierte Einträge sind blasser dargestellt, bleiben aber vollständig erhalten.",
                        )
                    }
                    if (history.isEmpty()) {
                        item {
                            EmptyState("Noch keine History", "Sobald du eine Dose hinzufügst, bleibt sie dauerhaft in dieser Übersicht.")
                        }
                    } else {
                        items(history, key = { "history-${it.id}" }) { can ->
                            HistoryCanEntry(
                                can = can,
                                eventCount = eventsByCan[can.id].orEmpty().size,
                                onClick = { onOpenCan(can.id) },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(contentPadding))
    }

    canPendingDeletion?.let { can ->
        AlertDialog(
            onDismissRequest = { canPendingDeletion = null },
            icon = {
                Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Dose endgültig löschen?") },
            text = {
                Text(
                    "„${can.colorName}“ wird aus Archiv, History und Statistiken entfernt. " +
                        "Diese Aktion kann nicht rückgängig gemacht werden.",
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        canPendingDeletion = null
                    },
                ) {
                    Text("Abbrechen")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        canPendingDeletion = null
                        sounds.play(UiSoundEffect.DESTRUCTIVE)
                        scope.launch {
                            val deleted = repository.deleteArchivedPermanently(can.id)
                            snackbar.showSnackbar(
                                message = if (deleted) {
                                    "${can.colorName} wurde endgültig gelöscht"
                                } else {
                                    "Dose konnte nicht gelöscht werden"
                                },
                                withDismissAction = true,
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Endgültig löschen")
                }
            },
        )
    }
}

@Composable
private fun StorageSectionSelector(
    selected: StorageSection,
    archiveCount: Int,
    historyCount: Int,
    onSelected: (StorageSection) -> Unit,
) {
    val sounds = LocalCanVaultSounds.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StorageSectionButton(
            title = "Archiv",
            count = archiveCount,
            icon = Icons.Rounded.Archive,
            selected = selected == StorageSection.ARCHIVE,
            onClick = {
                sounds.play(UiSoundEffect.STANDARD)
                onSelected(StorageSection.ARCHIVE)
            },
            modifier = Modifier.weight(1f),
        )
        StorageSectionButton(
            title = "History",
            count = historyCount,
            icon = Icons.Rounded.History,
            selected = selected == StorageSection.HISTORY,
            onClick = {
                sounds.play(UiSoundEffect.STANDARD)
                onSelected(StorageSection.HISTORY)
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StorageSectionButton(
    title: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(108.dp)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(27.dp),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun HistoryStats(stats: StorageStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 10.dp)) {
                Text("Deine Gesamtstatistik", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Gesamter Zeitraum · Verbrauchswerte sind Schätzungen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StatPair("Jemals besessen", stats.allTimeCanCount.toString(), "Aktuell im Bestand", stats.currentCanCount.toString())
        StatPair("Archiviert", stats.archivedCanCount.toString(), "Leer", stats.emptyCanCount.toString())
        StatPair("Gesamtausgaben", formatCurrency(stats.totalSpentCents), "Aktueller Wert", formatCurrency(stats.currentInventoryValueCents))
        StatPair("Ø Preis", stats.averagePriceCents?.let(::formatCurrency) ?: "–", "Teuerste Dose", stats.mostExpensiveCanCents?.let(::formatCurrency) ?: "–")
        StatPair("Verbraucht (geschätzt)", formatMl(stats.estimatedUsedVolumeMl), "Noch vorhanden", formatMl(stats.currentRemainingVolumeMl))
        StatPair("Verschiedene Farben", stats.distinctColorCount.toString(), "Ungeöffnet", "${stats.unopenedPercent} %")
        HistoryHighlights(stats)
    }
}

@Composable
private fun StatPair(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(firstLabel, firstValue, Modifier.weight(1f))
        StatCard(secondLabel, secondValue, Modifier.weight(1f))
    }
}

@Composable
private fun HistoryHighlights(stats: StorageStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Highlights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HighlightRow("Meistgekaufte Marke", stats.topBrandId?.let(::brandName) ?: "Noch keine Daten")
            HorizontalDivider()
            HighlightRow("Meistgekaufte Linie", stats.topLineId?.let(::lineName) ?: "Noch keine Daten")
            HorizontalDivider()
            HighlightRow("Meistverbrauchte Farbe", stats.topUsedColorName ?: "Noch keine Nutzung erfasst")
            HorizontalDivider()
            HighlightRow("Ø Nutzungsdauer", stats.averageUsageDays?.let { "$it Tage" } ?: "Noch nicht berechenbar")
            HorizontalDivider()
            HighlightRow("Insgesamt gekauft", formatMl(stats.purchasedVolumeMl))
        }
    }
}

@Composable
private fun HighlightRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MonthlyActivityCard(months: List<StorageMonthStat>) {
    val maximum = months.maxOfOrNull(StorageMonthStat::addedCanCount)?.coerceAtLeast(1) ?: 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Letzte 6 Monate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Neu erfasste Dosen und Ausgaben pro Monat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            months.forEach { month ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            month.yearMonth.format(DateTimeFormatter.ofPattern("MMM yy", Locale.GERMANY)),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            "${month.addedCanCount} Dosen · ${formatCurrency(month.spentCents)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { month.addedCanCount / maximum.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCanEntry(
    can: CanItem,
    eventCount: Int,
    onClick: () -> Unit,
) {
    val archived = can.status == CanStatus.ARCHIVED
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        CanCard(
            can = can,
            onClick = onClick,
            modifier = Modifier.alpha(if (archived) 0.56f else 1f),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Erfasst ${formatDate(can.acquiredAt)} · $eventCount Ereignis${if (eventCount == 1) "" else "se"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (archived) {
                Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "Im Archiv",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatCurrency(cents: Int): String =
    NumberFormat.getCurrencyInstance(Locale.GERMANY).format(cents / 100.0)

private fun formatMl(value: Int): String = when {
    value >= 1_000 -> String.format(Locale.GERMANY, "%.1f l", value / 1_000.0)
    else -> "${NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)} ml"
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMANY).format(Date(timestamp))
