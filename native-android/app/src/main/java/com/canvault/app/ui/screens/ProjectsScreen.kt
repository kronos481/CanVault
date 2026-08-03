package com.canvault.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.data.CanProject
import com.canvault.app.data.CreateProjectRequest
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.ProjectRepository
import com.canvault.app.data.ProjectStatus
import com.canvault.app.data.ProjectStats
import com.canvault.app.data.calculateProjectStats
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import com.canvault.app.ui.theme.CanVaultColors
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProjectsScreen(
    projectRepository: ProjectRepository,
    inventoryRepository: InventoryRepository,
    contentPadding: PaddingValues,
    onOpenProject: (String) -> Unit,
    onOpenMore: () -> Unit,
) {
    val projectSnapshot by projectRepository.snapshot.collectAsStateWithLifecycle()
    val inventorySnapshot by inventoryRepository.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val sounds = LocalCanVaultSounds.current
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val projects = remember(projectSnapshot.projects) {
        projectSnapshot.projects.sortedWith(
            compareBy<CanProject> { it.status == ProjectStatus.ARCHIVED }
                .thenByDescending(CanProject::updatedAt),
        )
    }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Projekte", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Aktionen planen, Bestand reservieren und Einkauf im Blick behalten",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {
                        sounds.play(UiSoundEffect.NAVIGATION)
                        onOpenMore()
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.MoreHoriz, contentDescription = "Mehr öffnen")
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    ProjectEmptyState(onCreate = {
                        sounds.play(UiSoundEffect.PRIMARY)
                        showCreateDialog = true
                    })
                }
            } else {
                val activeCount = projects.count { it.status !in setOf(ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProjectSummaryTile("Aktiv", activeCount.toString(), Modifier.weight(1f))
                        ProjectSummaryTile("Gesamt", projects.size.toString(), Modifier.weight(1f))
                    }
                }
                items(projects, key = CanProject::id) { project ->
                    ProjectCard(
                        project = project,
                        stats = calculateProjectStats(project, inventorySnapshot.cans),
                        onClick = {
                            sounds.play(UiSoundEffect.NAVIGATION)
                            onOpenProject(project.id)
                        },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                sounds.play(UiSoundEffect.PRIMARY)
                showCreateDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Neues Projekt") },
        )
    }

    if (showCreateDialog) {
        ProjectDetailsDialog(
            initial = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                scope.launch {
                    val id = projectRepository.create(request)
                    sounds.play(UiSoundEffect.SUCCESS)
                    showCreateDialog = false
                    onOpenProject(id)
                }
            },
        )
    }
}

@Composable
private fun ProjectEmptyState(onCreate: () -> Unit) {
    Card(
        onClick = onCreate,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.Construction,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Plane deine nächste Aktion",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Wähle Dosen aus dem Lager, ergänze fehlende Farben und sieh sofort Kosten, Volumen und Reichweite.",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Projekt erstellen", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun ProjectSummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectCard(
    project: CanProject,
    stats: ProjectStats,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val meta = listOfNotNull(project.location, project.targetDate).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ProjectStatusPill(project.status)
            }

            if (project.targetAreaM2 != null) {
                val progress = ((stats.readyPercent ?: 0) / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                    color = if (stats.readyShortageMl == 0) MaterialTheme.colorScheme.primary else CanVaultColors.Warning,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    "${stats.readyPercent ?: 0} % sofort bereit · ${formatProjectArea(stats.plannedCoverageM2)} geplant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProjectMiniMetric("Kosten", formatProjectMoney(stats.projectedCostCents), Modifier.weight(1f))
                ProjectMiniMetric("Material", formatProjectVolume(stats.plannedMl), Modifier.weight(1f))
                ProjectMiniMetric("Kaufen", stats.openBuyUnits.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProjectMiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ProjectStatusPill(status: ProjectStatus) {
    val tint = when (status) {
        ProjectStatus.READY, ProjectStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        ProjectStatus.PLANNING -> CanVaultColors.Warning
        ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(status.projectLabel()) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = tint.copy(alpha = 0.14f),
            disabledLabelColor = tint,
        ),
        border = null,
    )
}

internal fun ProjectStatus.projectLabel(): String = when (this) {
    ProjectStatus.PLANNING -> "Planung"
    ProjectStatus.READY -> "Bereit"
    ProjectStatus.ACTIVE -> "Aktiv"
    ProjectStatus.COMPLETED -> "Abgeschlossen"
    ProjectStatus.ARCHIVED -> "Archiviert"
}

@Composable
internal fun ProjectDetailsDialog(
    initial: CanProject?,
    onDismiss: () -> Unit,
    onConfirm: (CreateProjectRequest) -> Unit,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var location by remember(initial?.id) { mutableStateOf(initial?.location.orEmpty()) }
    var targetArea by remember(initial?.id) { mutableStateOf(initial?.targetAreaM2?.toString().orEmpty()) }
    var coverage by remember(initial?.id) { mutableStateOf(initial?.coverageM2PerLiter?.toString() ?: "5,0") }
    var budget by remember(initial?.id) { mutableStateOf(initial?.budgetCents?.let { formatDecimal(it / 100.0) }.orEmpty()) }
    var targetDate by remember(initial?.id) { mutableStateOf(initial?.targetDate.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    val request = CreateProjectRequest(
        name = name,
        location = location,
        notes = notes,
        targetAreaM2 = targetArea.parseProjectDouble(),
        coverageM2PerLiter = coverage.parseProjectDouble() ?: 5.0,
        budgetCents = budget.parseProjectDouble()?.times(100)?.toInt(),
        targetDate = targetDate,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neues Projekt" else "Projekt bearbeiten") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Projektname *") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ort / Spot") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetArea,
                        onValueChange = { targetArea = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Ziel‑m²") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = coverage,
                        onValueChange = { coverage = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("m² / Liter") },
                        supportingText = { Text("Standard 5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Budget €") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Zieldatum") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notizen / Aufgaben") },
                    minLines = 3,
                    maxLines = 5,
                )
                Text(
                    "Die Flächenabdeckung ist eine Planungsschätzung. Untergrund, Deckkraft und Technik können den Verbrauch verändern.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(request) }, enabled = name.isNotBlank()) {
                Text(if (initial == null) "Projekt erstellen" else "Speichern")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

internal fun formatProjectMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale.GERMANY).format(cents / 100.0)

internal fun formatProjectVolume(ml: Int): String = if (ml < 1_000) {
    "$ml ml"
} else {
    "${formatDecimal(ml / 1_000.0)} L"
}

internal fun formatProjectArea(area: Double): String = "${formatDecimal(area)} m²"

private fun formatDecimal(value: Double): String =
    NumberFormat.getNumberInstance(Locale.GERMANY).apply { maximumFractionDigits = 2 }.format(value)

private fun String.parseProjectDouble(): Double? = trim().replace(',', '.').toDoubleOrNull()
