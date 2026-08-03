package com.canvault.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.data.AddProjectBuyItemRequest
import com.canvault.app.data.CanItem
import com.canvault.app.data.CanProject
import com.canvault.app.data.CanStatus
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.OfficialCanColor
import com.canvault.app.data.OfficialCanColorCatalog
import com.canvault.app.data.ProjectBuyItem
import com.canvault.app.data.ProjectRepository
import com.canvault.app.data.ProjectStatus
import com.canvault.app.data.ProjectStats
import com.canvault.app.data.SharedCatalogRepository
import com.canvault.app.data.VerifiedCatalogSnapshot
import com.canvault.app.data.brandName
import com.canvault.app.data.calculateProjectStats
import com.canvault.app.data.canCatalog
import com.canvault.app.data.catalogBrand
import com.canvault.app.data.catalogDisplayVolumeMl
import com.canvault.app.data.catalogLine
import com.canvault.app.data.lineName
import com.canvault.app.data.remainingVolumeMl
import com.canvault.app.data.resolveCanColorHex
import com.canvault.app.data.suggestPurchasePrice
import com.canvault.app.ui.components.BrandLogo
import com.canvault.app.ui.components.StatCard
import com.canvault.app.ui.components.safeCanColor
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import com.canvault.app.ui.theme.CanVaultColors
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectRepository: ProjectRepository,
    inventoryRepository: InventoryRepository,
    sharedCatalogRepository: SharedCatalogRepository,
    projectId: String,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val projectSnapshot by projectRepository.snapshot.collectAsStateWithLifecycle()
    val inventorySnapshot by inventoryRepository.snapshot.collectAsStateWithLifecycle()
    val catalogSnapshot by sharedCatalogRepository.snapshot.collectAsStateWithLifecycle()
    val project = projectSnapshot.projects.firstOrNull { it.id == projectId }
    val scope = rememberCoroutineScope()
    val sounds = LocalCanVaultSounds.current
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var showInventoryPicker by rememberSaveable { mutableStateOf(false) }
    var showBuyDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteProject by rememberSaveable { mutableStateOf(false) }
    var deleteBuyItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var moreExpanded by remember { mutableStateOf(false) }

    if (project == null) {
        ProjectMissingScreen(onBack)
        return
    }

    val selectedCans = remember(project.inventoryCanIds, inventorySnapshot.cans) {
        project.inventoryCanIds.mapNotNull { id -> inventorySnapshot.cans.firstOrNull { it.id == id } }
    }
    val occupiedByOtherProjects = remember(projectSnapshot.projects, project.id) {
        projectSnapshot.projects
            .filter { it.id != project.id && it.status !in setOf(ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED) }
            .flatMap(CanProject::inventoryCanIds)
            .toSet()
    }
    val stats = remember(project, inventorySnapshot.cans) {
        calculateProjectStats(project, inventorySnapshot.cans)
    }
    val palette = remember(selectedCans, project.buyItems) {
        buildList {
            selectedCans.forEach { can -> add(can.colorName to resolveCanColorHex(can)) }
            project.buyItems.forEach { item -> add(item.colorName to item.customHex) }
        }.filter { it.second != null }.distinctBy { it.first.lowercase() }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    sounds.play(UiSoundEffect.NAVIGATION)
                    onBack()
                }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück zu Projekten")
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(project.status.projectLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    sounds.play(UiSoundEffect.STANDARD)
                    showEdit = true
                }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Projekt bearbeiten")
                }
                Box {
                    IconButton(onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        moreExpanded = true
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Projektaktionen")
                    }
                    DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Projekt duplizieren") },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                            onClick = {
                                moreExpanded = false
                                scope.launch {
                                    val newId = projectRepository.duplicate(project.id)
                                    if (newId != null) {
                                        sounds.play(UiSoundEffect.SUCCESS)
                                        onOpenProject(newId)
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Projekt löschen", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                moreExpanded = false
                                showDeleteProject = true
                            },
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProjectStatus.entries) { status ->
                        FilterChip(
                            selected = project.status == status,
                            onClick = {
                                sounds.play(UiSoundEffect.STANDARD)
                                scope.launch { projectRepository.setStatus(project.id, status) }
                            },
                            label = { Text(status.projectLabel()) },
                        )
                    }
                }
            }

            item { ProjectReadinessCard(project, stats) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Projektwert", formatProjectMoney(stats.projectedCostCents), Modifier.weight(1f))
                    StatCard("Fläche geplant", formatProjectArea(stats.plannedCoverageM2), Modifier.weight(1f), CanVaultColors.Warning)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Material", formatProjectVolume(stats.plannedMl), Modifier.weight(1f))
                    StatCard("Dosen geplant", (stats.selectedCanCount + project.buyItems.sumOf(ProjectBuyItem::quantity)).toString(), Modifier.weight(1f))
                }
            }

            if (project.budgetCents != null) {
                item { ProjectBudgetCard(project, stats) }
            }

            if (palette.isNotEmpty()) {
                item { ProjectPaletteCard(palette) }
            }

            item {
                SectionHeader(
                    title = "Aus deinem Lager",
                    subtitle = "${selectedCans.size} Dosen · ${formatProjectVolume(stats.availableMl)} verfügbar",
                    actionLabel = "Auswählen",
                    actionIcon = Icons.Rounded.Inventory2,
                    onAction = {
                        sounds.play(UiSoundEffect.STANDARD)
                        showInventoryPicker = true
                    },
                )
            }
            if (selectedCans.isEmpty()) {
                item {
                    HintCard("Noch keine Lagerdosen eingeplant", "Wähle vorhandene Dosen aus. Der aktuelle Füllstand wird automatisch in ml und m² berechnet.")
                }
            } else {
                items(selectedCans, key = CanItem::id) { can ->
                    ProjectInventoryCanRow(
                        can = can,
                        onRemove = {
                            sounds.play(UiSoundEffect.STANDARD)
                            scope.launch {
                                projectRepository.setInventoryCans(project.id, project.inventoryCanIds - can.id)
                            }
                        },
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Noch zu kaufen",
                    subtitle = "${stats.openBuyUnits} offen · ${formatProjectMoney(stats.outstandingCostCents)}",
                    actionLabel = "Dose",
                    actionIcon = Icons.Rounded.AddShoppingCart,
                    onAction = {
                        sounds.play(UiSoundEffect.PRIMARY)
                        showBuyDialog = true
                    },
                )
            }
            if (project.buyItems.isEmpty()) {
                item {
                    HintCard("Kaufliste ist leer", "Ergänze fehlende Dosen. Herstellerfarben und Durchschnittspreise werden automatisch vorgeschlagen.")
                }
            } else {
                items(project.buyItems, key = ProjectBuyItem::id) { item ->
                    ProjectBuyItemRow(
                        item = item,
                        onToggle = {
                            sounds.play(if (item.purchased) UiSoundEffect.STANDARD else UiSoundEffect.SUCCESS)
                            scope.launch { projectRepository.toggleBuyItem(project.id, item.id) }
                        },
                        onDelete = {
                            sounds.play(UiSoundEffect.STANDARD)
                            deleteBuyItemId = item.id
                        },
                    )
                }
            }

            item { ProjectPlanCard(project) }
        }
    }

    if (showEdit) {
        ProjectDetailsDialog(
            initial = project,
            onDismiss = { showEdit = false },
            onConfirm = { request ->
                scope.launch {
                    projectRepository.updateDetails(project.id, request)
                    sounds.play(UiSoundEffect.SUCCESS)
                    showEdit = false
                }
            },
        )
    }

    if (showInventoryPicker) {
        InventoryProjectPicker(
            project = project,
            inventory = inventorySnapshot.cans,
            occupiedCanIds = occupiedByOtherProjects,
            onDismiss = { showInventoryPicker = false },
            onApply = { selected ->
                scope.launch {
                    projectRepository.setInventoryCans(project.id, selected)
                    sounds.play(UiSoundEffect.SUCCESS)
                    showInventoryPicker = false
                }
            },
        )
    }

    if (showBuyDialog) {
        BuyCanDialog(
            catalogSnapshot = catalogSnapshot,
            onDismiss = { showBuyDialog = false },
            onConfirm = { request ->
                scope.launch {
                    projectRepository.addBuyItem(project.id, request)
                    sounds.play(UiSoundEffect.SUCCESS)
                    showBuyDialog = false
                }
            },
        )
    }

    if (showDeleteProject) {
        AlertDialog(
            onDismissRequest = { showDeleteProject = false },
            title = { Text("Projekt endgültig löschen?") },
            text = { Text("Das Inventar bleibt unverändert. Projektplan und Kaufliste werden dauerhaft entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        projectRepository.delete(project.id)
                        sounds.play(UiSoundEffect.DESTRUCTIVE)
                        onBack()
                    }
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteProject = false }) { Text("Abbrechen") } },
        )
    }

    deleteBuyItemId?.let { itemId ->
        AlertDialog(
            onDismissRequest = { deleteBuyItemId = null },
            title = { Text("Von der Kaufliste entfernen?") },
            text = { Text("Nur dieser Eintrag wird gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        projectRepository.removeBuyItem(project.id, itemId)
                        sounds.play(UiSoundEffect.DESTRUCTIVE)
                        deleteBuyItemId = null
                    }
                }) { Text("Entfernen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteBuyItemId = null }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun ProjectMissingScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text("Projekt nicht gefunden", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onBack) { Text("Zurück") }
    }
}

@Composable
private fun ProjectReadinessCard(project: CanProject, stats: ProjectStats) {
    val (title, body, icon, accent) = when {
        project.targetAreaM2 == null -> ReadinessView(
            "Ziel-Fläche ergänzen",
            "Mit einer Zielgröße kann CANVAULT fehlendes Material und Fortschritt berechnen.",
            Icons.Rounded.WarningAmber,
            CanVaultColors.Warning,
        )
        stats.readyShortageMl == 0 && stats.openBuyUnits == 0 -> ReadinessView(
            "Bereit für die Aktion",
            "Dein sofort verfügbares Material deckt die geplante Fläche ab.",
            Icons.Rounded.CheckCircle,
            MaterialTheme.colorScheme.primary,
        )
        stats.shortageMl == 0 -> ReadinessView(
            "Plan vollständig · Einkauf offen",
            "Noch ${stats.openBuyUnits} Dosen kaufen. Danach stehen voraussichtlich ${formatProjectArea(stats.plannedCoverageM2)} bereit.",
            Icons.Rounded.AddShoppingCart,
            CanVaultColors.Warning,
        )
        else -> ReadinessView(
            "Noch ${formatProjectVolume(stats.shortageMl)} einplanen",
            "Aus Lager und Kaufliste reichen aktuell für ${formatProjectArea(stats.plannedCoverageM2)}.",
            Icons.Rounded.WarningAmber,
            MaterialTheme.colorScheme.error,
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (project.targetAreaM2 != null) {
                LinearProgressIndicator(
                    progress = { ((stats.readyPercent ?: 0) / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Row(Modifier.fillMaxWidth()) {
                    Text("Sofort ${formatProjectArea(stats.readyCoverageM2)}", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    Text("Ziel ${formatProjectArea(project.targetAreaM2)}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private data class ReadinessView(
    val title: String,
    val body: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
)

@Composable
private fun ProjectBudgetCard(project: CanProject, stats: ProjectStats) {
    val budget = project.budgetCents?.toLong() ?: return
    val remaining = stats.budgetRemainingCents ?: budget
    val overBudget = remaining < 0
    val progress = if (budget == 0L) 1f else (stats.projectedCostCents.toFloat() / budget).coerceIn(0f, 1f)
    val accent = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Geplant ${formatProjectMoney(stats.projectedCostCents)} von ${formatProjectMoney(budget)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (overBudget) "${formatProjectMoney(-remaining)} drüber" else "${formatProjectMoney(remaining)} frei",
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun ProjectPaletteCard(colors: List<Pair<String, String?>>) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text("Projektfarben", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${colors.size} exakte Farbtöne aus Lager und Kaufliste", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(colors, key = { it.first }) { (name, hex) ->
                    Column(Modifier.width(82.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(safeCanColor(hex)))
                        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = onAction, modifier = Modifier.height(48.dp)) {
            Icon(actionIcon, contentDescription = null)
            Text(actionLabel, Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun HintCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectInventoryCanRow(can: CanItem, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(8.dp).height(88.dp).background(safeCanColor(can)))
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp).weight(1f)) {
                BrandLogo(can.brandId, Modifier.height(24.dp).widthIn(max = 112.dp))
                Text(lineName(can.canLineId), fontWeight = FontWeight.SemiBold)
                Text(
                    "${can.colorName}${can.colorCode?.let { " · $it" }.orEmpty()} · ${formatProjectVolume(can.remainingVolumeMl())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "${can.colorName} aus Projekt entfernen")
            }
        }
    }
}

@Composable
private fun ProjectBuyItemRow(item: ProjectBuyItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (item.purchased) 0.62f else 1f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.purchased, onCheckedChange = { onToggle() })
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(safeCanColor(item.customHex)),
            )
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Text(
                    "${brandName(item.brandId)} · ${lineName(item.canLineId)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    item.colorName,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    "${item.quantity} × ${item.volumeMl} ml · ${formatProjectMoney((item.unitPriceCents ?: 0).toLong() * item.quantity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "${item.colorName} von Kaufliste entfernen")
            }
        }
    }
}

@Composable
private fun ProjectPlanCard(project: CanProject) {
    val details = listOfNotNull(
        project.location?.let { "Ort: $it" },
        project.targetDate?.let { "Zieldatum: $it" },
        project.notes,
    )
    if (details.isEmpty()) return
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Plan & Notizen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            details.forEach { detail -> Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                "Berechnung mit ${NumberFormat.getNumberInstance(Locale.GERMANY).format(project.coverageM2PerLiter)} m² pro Liter",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryProjectPicker(
    project: CanProject,
    inventory: List<CanItem>,
    occupiedCanIds: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit,
) {
    var selected by remember(project.id, project.inventoryCanIds) { mutableStateOf(project.inventoryCanIds.toSet()) }
    val available = remember(inventory) {
        inventory.filter { it.status != CanStatus.ARCHIVED && it.remainingVolumeMl() > 0 }
            .sortedByDescending(CanItem::remainingVolumeMl)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.86f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Dosen aus dem Lager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Füllstand und exakte Farbe werden live übernommen. Dosen in anderen aktiven Projekten bleiben gesperrt.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (available.isEmpty()) {
                HintCard("Kein Material verfügbar", "Füge zuerst eine gefüllte Dose zum Inventar hinzu.")
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(available, key = CanItem::id) { can ->
                        val occupied = can.id in occupiedCanIds
                        val checked = can.id in selected
                        Card(
                            onClick = {
                                if (!occupied) selected = if (checked) selected - can.id else selected + can.id
                            },
                            enabled = !occupied,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = null, enabled = !occupied)
                                Box(Modifier.size(36.dp).clip(CircleShape).background(safeCanColor(can)))
                                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text("${brandName(can.brandId)} · ${can.colorName}", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (occupied) "Bereits in anderem Projekt" else "${lineName(can.canLineId)} · ${formatProjectVolume(can.remainingVolumeMl())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (occupied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onApply(selected) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("${selected.size} Dosen übernehmen") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuyCanDialog(
    catalogSnapshot: VerifiedCatalogSnapshot,
    onDismiss: () -> Unit,
    onConfirm: (AddProjectBuyItemRequest) -> Unit,
) {
    var brandId by remember { mutableStateOf(canCatalog.first().id) }
    var lineId by remember { mutableStateOf(canCatalog.first().lines.first().id) }
    var colorName by remember { mutableStateOf("") }
    var colorCode by remember { mutableStateOf<String?>(null) }
    var customHex by remember { mutableStateOf<String?>(null) }
    var volume by remember { mutableStateOf(catalogDisplayVolumeMl(lineId).toString()) }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var priceManuallyEdited by remember { mutableStateOf(false) }
    val brand = catalogBrand(brandId) ?: canCatalog.first()
    val line = catalogLine(lineId) ?: brand.lines.first()
    val officialColors = remember(lineId) { OfficialCanColorCatalog.colorsForLine(lineId) }
    val priceSuggestion = remember(catalogSnapshot, lineId, volume) {
        catalogSnapshot.suggestPurchasePrice(lineId, volume.toIntOrNull() ?: catalogDisplayVolumeMl(lineId))
    }

    LaunchedEffect(lineId) {
        val first = OfficialCanColorCatalog.colorsForLine(lineId).firstOrNull()
        colorName = first?.colorName ?: line.defaultColorName.orEmpty()
        colorCode = first?.colorCode ?: first?.productCode ?: line.defaultColorCode
        customHex = first?.hex ?: line.defaultColorHex
        volume = catalogDisplayVolumeMl(lineId).toString()
        priceManuallyEdited = false
    }
    LaunchedEffect(priceSuggestion?.priceEurCents, priceManuallyEdited) {
        if (!priceManuallyEdited) {
            price = priceSuggestion?.priceEurCents?.let(::formatProjectPriceInput).orEmpty()
        }
    }

    val request = AddProjectBuyItemRequest(
        brandId = brandId,
        canLineId = lineId,
        colorName = colorName,
        colorCode = colorCode,
        customHex = customHex,
        volumeMl = volume.toIntOrNull() ?: catalogDisplayVolumeMl(lineId),
        quantity = quantity.toIntOrNull() ?: 1,
        unitPriceCents = price.replace(',', '.').toDoubleOrNull()?.times(100)?.toInt(),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dose zur Kaufliste") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProjectBrandDropdown(
                    selectedId = brandId,
                    selected = brand.displayName,
                    options = canCatalog.map { it.id to it.displayName },
                    onSelect = { selectedId ->
                        brandId = selectedId
                        lineId = catalogBrand(selectedId)?.lines?.firstOrNull()?.id ?: lineId
                    },
                )
                ProjectOptionDropdown(
                    label = "Dosenlinie",
                    selected = line.displayName,
                    options = brand.lines.map { it.id to it.displayName },
                    onSelect = { lineId = it },
                )
                if (officialColors.isNotEmpty()) {
                    ProjectColorDropdown(
                        selected = colorName.ifBlank { "Farbe wählen" },
                        colors = officialColors,
                        onSelect = { color ->
                            colorName = color.colorName
                            colorCode = color.colorCode ?: color.productCode
                            customHex = color.hex
                        },
                    )
                } else {
                    OutlinedTextField(
                        value = colorName,
                        onValueChange = { colorName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Farbname") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = customHex.orEmpty(),
                        onValueChange = { customHex = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("HEX-Farbe") },
                        placeholder = { Text("#58E4C2") },
                        singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = volume,
                        onValueChange = { volume = it.filter(Char::isDigit) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Volumen ml") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter(Char::isDigit) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Anzahl") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        priceManuallyEdited = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Preis pro Dose €") },
                    supportingText = {
                        Text(if (priceSuggestion == null) "Optional" else "Durchschnittspreis vorgeschlagen · überschreibbar")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text(
                    OfficialCanColorCatalog.accuracyNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(request) },
                enabled = colorName.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0 && (volume.toIntOrNull() ?: 0) > 0,
            ) { Text("Hinzufügen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectBrandDropdown(
    selectedId: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Marke") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandLogo(selectedId, Modifier.width(70.dp).height(28.dp))
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    trailingIcon = { BrandLogo(id, Modifier.width(88.dp).height(30.dp)) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectOptionDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onSelect(id)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectColorDropdown(
    selected: String,
    colors: List<OfficialCanColor>,
    onSelect: (OfficialCanColor) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Herstellerfarbe") },
            leadingIcon = {
                val hex = colors.firstOrNull { it.colorName == selected }?.hex
                Box(Modifier.size(28.dp).clip(CircleShape).background(safeCanColor(hex)))
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            colors.forEach { color ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(color.colorName)
                            Text(color.colorCode ?: color.productCode.orEmpty(), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    leadingIcon = { Box(Modifier.size(28.dp).clip(CircleShape).background(safeCanColor(color.hex))) },
                    onClick = {
                        onSelect(color)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatProjectPriceInput(cents: Int): String =
    String.format(Locale.GERMANY, "%.2f", cents / 100.0)
