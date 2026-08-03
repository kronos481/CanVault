package com.canvault.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.data.CanItem
import com.canvault.app.data.CanStatus
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.brandName
import com.canvault.app.data.inventoryColorComparator
import com.canvault.app.data.lineName
import com.canvault.app.ui.components.CanCard
import com.canvault.app.ui.components.EmptyState
import com.canvault.app.ui.components.label
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect

private enum class SortMode(val label: String) {
    NEWEST("Neueste"),
    NAME("Name"),
    COLOR("Nach Farbe"),
    FILL("Füllstand"),
}

@Composable
fun InventoryScreen(
    repository: InventoryRepository,
    contentPadding: PaddingValues,
    onOpenCan: (String) -> Unit,
    onScan: () -> Unit,
    onOpenColorCombo: () -> Unit,
) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf<CanStatus?>(null) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.NEWEST) }
    val focusManager = LocalFocusManager.current
    val sounds = LocalCanVaultSounds.current

    val visible = remember(snapshot.cans, query, selectedStatus, sortMode) {
        snapshot.cans
            .asSequence()
            .filter { it.status != CanStatus.ARCHIVED }
            .filter { selectedStatus == null || it.status == selectedStatus }
            .filter { can ->
                query.isBlank() || listOf(
                    brandName(can.brandId),
                    lineName(can.canLineId),
                    can.colorName,
                    can.colorCode.orEmpty(),
                    can.externalBarcode.orEmpty(),
                ).any { it.contains(query, ignoreCase = true) }
            }
            .let { sequence ->
                when (sortMode) {
                    SortMode.NEWEST -> sequence.sortedByDescending(CanItem::createdAt)
                    SortMode.NAME -> sequence.sortedBy { "${brandName(it.brandId)} ${it.colorName}".lowercase() }
                    SortMode.COLOR -> sequence.sortedWith(inventoryColorComparator)
                    SortMode.FILL -> sequence.sortedByDescending { it.fillPercent ?: -1 }
                }
            }
            .toList()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Inventar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${visible.size} von ${snapshot.cans.count { it.status != CanStatus.ARCHIVED }} Dosen",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onScan) {
                    Icon(Icons.Rounded.DocumentScanner, contentDescription = null)
                    Text("Barcode", Modifier.padding(start = 8.dp))
                }
            }

            FilledTonalButton(
                onClick = onOpenColorCombo,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Rounded.Palette, contentDescription = null)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("Color Combo", fontWeight = FontWeight.Bold)
                    Text(
                        "Paletten aus deinem echten Bestand",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Suchen") },
                placeholder = { Text("Marke, Linie, Farbe oder Code") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            sounds.play(UiSoundEffect.STANDARD)
                            query = ""
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Suche leeren")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        selectedStatus = null
                    },
                    label = { Text("Alle") },
                )
            }
            items(CanStatus.entries.filter { it != CanStatus.ARCHIVED }) { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        selectedStatus = if (selectedStatus == status) null else status
                    },
                    label = { Text(status.label()) },
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SortMode.entries) { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        sortMode = mode
                    },
                    label = { Text(mode.label) },
                    leadingIcon = if (mode == SortMode.COLOR) {
                        { Icon(Icons.Rounded.Palette, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }

        AnimatedContent(targetState = visible.isEmpty(), label = "inventory-state") { empty ->
            if (empty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    EmptyState(
                        title = if (snapshot.cans.isEmpty()) "Noch keine Dosen" else "Keine Treffer",
                        body = if (snapshot.cans.isEmpty()) {
                            "Über Scan oder Hinzufügen legst du deine erste Dose an."
                        } else {
                            "Passe Suche oder Filter an."
                        },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().animateContentSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = contentPadding.calculateBottomPadding() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.id }) { can ->
                        CanCard(can = can, onClick = { onOpenCan(can.id) })
                    }
                }
            }
        }
    }
}
