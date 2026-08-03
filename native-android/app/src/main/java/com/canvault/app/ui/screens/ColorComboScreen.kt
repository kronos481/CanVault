package com.canvault.app.ui.screens

import android.graphics.Color.parseColor
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.data.ColorComboAnalysis
import com.canvault.app.data.ColorHarmonyEngine
import com.canvault.app.data.ColorHarmonyPalette
import com.canvault.app.data.CanItem
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.OfficialCanColorCatalog
import com.canvault.app.data.PaintRole
import com.canvault.app.data.PaletteSwatch
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import com.canvault.app.ui.sound.soundClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private enum class ColorComboMode(val label: String) {
    INVENTORY("Nur Bestand"),
    ADD_COLOR("Add Color"),
}

private data class CompletedColorCombo(
    val cans: List<CanItem>,
    val mode: ColorComboMode,
    val toneCount: Int,
    val analysis: ColorComboAnalysis,
)

private const val addColorCalculationDisplayMillis = 180L
private const val inventoryCalculationDisplayMillis = 220L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorComboScreen(
    repository: InventoryRepository,
    onBack: () -> Unit,
) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val sounds = LocalCanVaultSounds.current
    val backClick = soundClick(onClick = onBack)
    var mode by remember { mutableStateOf(ColorComboMode.INVENTORY) }
    var toneCount by rememberSaveable { mutableIntStateOf(5) }
    var completed by remember { mutableStateOf<CompletedColorCombo?>(null) }
    val analysis = completed?.takeIf { result ->
        result.cans == snapshot.cans && result.mode == mode && result.toneCount == toneCount
    }?.analysis

    LaunchedEffect(snapshot.cans, mode, toneCount) {
        val requestedCans = snapshot.cans
        val requestedMode = mode
        val requestedToneCount = toneCount
        val startedAt = SystemClock.elapsedRealtime()
        val generated = withContext(Dispatchers.Default) {
            ColorHarmonyEngine.analyze(
                cans = requestedCans,
                includeMissingColors = requestedMode == ColorComboMode.ADD_COLOR,
                toneCount = requestedToneCount,
            )
        }
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val minimumDisplay = if (requestedMode == ColorComboMode.ADD_COLOR) {
            addColorCalculationDisplayMillis
        } else {
            inventoryCalculationDisplayMillis
        }
        delay((minimumDisplay - elapsed).coerceAtLeast(0L))
        completed = CompletedColorCombo(
            cans = requestedCans,
            mode = requestedMode,
            toneCount = requestedToneCount,
            analysis = generated,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Color Combo", fontWeight = FontWeight.Bold)
                        Text(
                            "Automatisch aus deinem Bestand",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = backClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ToneCountSelector(
                    toneCount = toneCount,
                    onToneCountCommitted = { selectedToneCount ->
                        if (toneCount != selectedToneCount) {
                            toneCount = selectedToneCount
                            sounds.play(UiSoundEffect.COLOR)
                        }
                    },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Modus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ColorComboMode.entries.forEach { option ->
                            FilterChip(
                                selected = mode == option,
                                onClick = {
                                    if (mode != option) {
                                        sounds.play(UiSoundEffect.SHAKE)
                                        mode = option
                                    }
                                },
                                label = { Text(option.label) },
                                leadingIcon = {
                                    Icon(
                                        if (option == ColorComboMode.INVENTORY) Icons.Rounded.CheckCircle else Icons.Rounded.AddShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                            )
                        }
                    }
                    Text(
                        if (mode == ColorComboMode.INVENTORY) {
                            "Nur vorhandene Dosen. Die App verteilt sie automatisch auf Rollen und verwirft jede unlesbare Dunkel-auf-Dunkel-Kante."
                        } else {
                            "Vorhandene Farben zuerst, danach echte kaufbare Herstellerfarben. Fehlende Dosen sind gestrichelt markiert."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (analysis == null) {
                item { ColorCalculationLoading(mode = mode, toneCount = toneCount) }
            } else {
                item { ColorSummaryCard(analysis) }
                if (analysis.unresolvedCanCount > 0) {
                    item { UnresolvedColorNotice(analysis.unresolvedCanCount) }
                }
                if (analysis.inventoryColors.isEmpty()) {
                    item { EmptyColorCombo() }
                } else {
                    item {
                        Text(
                            if (mode == ColorComboMode.INVENTORY) "Generierte Bestands-Combos" else "Generierte Kauf-Combos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    item {
                        AnimatedContent(
                            targetState = mode,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(160)) using SizeTransform(clip = false)
                            },
                            label = "color-combo-mode",
                        ) { targetMode ->
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (analysis.palettes.isEmpty()) {
                                    NoMatchingPalettes(targetMode, analysis)
                                } else {
                                    analysis.palettes.forEach { palette ->
                                        PaletteCard(palette)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (analysis != null) {
                item {
                    Text(
                        OfficialCanColorCatalog.accuracyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorCalculationLoading(
    mode: ColorComboMode,
    toneCount: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Farbkombinationen werden berechnet"
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(46.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 4.dp,
            )
            Column(
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Farben werden berechnet …", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (mode == ColorComboMode.INVENTORY) {
                        "$toneCount passende Rollen werden in deinem Bestand gesucht."
                    } else {
                        "Bestand und echte Katalogfarben werden für $toneCount Rollen geprüft."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ColorSummaryCard(analysis: ColorComboAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text("Dein Farbpotenzial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${analysis.inventoryColors.size} exakte Farben · ${formatMl(analysis.totalEffectiveMl)} verfügbar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${formatCandidateCount(analysis.knowledgeBaseCandidateCount)} mögliche Katalog-Kombinationen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ToneCountSelector(
    toneCount: Int,
    onToneCountCommitted: (Int) -> Unit,
) {
    var previewToneCount by remember(toneCount) { mutableIntStateOf(toneCount) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Anzahl Farbtöne",
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "$previewToneCount Töne",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Slider(
                value = previewToneCount.toFloat(),
                onValueChange = { previewToneCount = it.roundToInt().coerceIn(2, 7) },
                onValueChangeFinished = { onToneCountCommitted(previewToneCount) },
                valueRange = 2f..7f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("2 · minimal", style = MaterialTheme.typography.labelSmall)
                Text("7 · kompletter Aufbau", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                toneCountHint(previewToneCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnresolvedColorNotice(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("$count Dose${if (count == 1) "" else "n"} ohne auflösbaren Farbwert", fontWeight = FontWeight.SemiBold)
                Text(
                    "Diese Dose wird nicht geraten. Beim nächsten Bearbeiten reichen ein vollständiger Hersteller-Farbname oder Farbcode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyColorCombo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(42.dp))
            Text("Noch keine exakte Farbe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Sobald im Inventar eine Dose mit erkanntem Hersteller-Farbname, Farbcode oder eigenem HEX liegt, entstehen die Paletten automatisch.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoMatchingPalettes(mode: ColorComboMode, analysis: ColorComboAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (mode == ColorComboMode.INVENTORY) "Noch keine vollständige Harmonie" else "Keine verifizierte Ergänzung gefunden",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (mode == ColorComboMode.INVENTORY) {
                    if (analysis.inventoryColors.size < analysis.requestedToneCount) {
                        "Du hast ${analysis.inventoryColors.size} eindeutige Farben, aber ${analysis.requestedToneCount} Töne gewählt. Stelle den Slider niedriger oder nutze Add Color."
                    } else {
                        "Keine vorhandene Anordnung erreicht an allen wichtigen Kanten mindestens 3:1 Kontrast. Die App zeigt bewusst keine Dunkel-auf-Dunkel-Palette."
                    }
                } else {
                    "Für diese Tonanzahl wurde keine Kombination gefunden, die Rollen, Fade-Regeln und mindestens 3:1 Kantenkontrast gleichzeitig erfüllt."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaletteCard(palette: ColorHarmonyPalette) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(palette.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        palette.rule,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                PaletteStatus(palette)
            }
            Text(
                palette.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PaletteStrip(palette.swatches)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        "Kante ${formatRatio(palette.minimumEdgeContrastRatio)}:1",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (palette.hasFillFade) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            "Fill-Fade",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                if (palette.fillColorCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "${palette.fillColorCount}× Fill",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.swatches.forEach { swatch -> SwatchLegend(swatch) }
            }
        }
    }
}

@Composable
private fun PaletteStatus(palette: ColorHarmonyPalette) {
    val (containerColor, contentColor) = when {
        palette.scorePercent >= 80 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        palette.scorePercent >= 60 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = Modifier.semantics {
            contentDescription = "Harmonie ${palette.scorePercent} von 100 Prozent"
        },
        shape = RoundedCornerShape(99.dp),
        color = containerColor,
    ) {
        Text(
            "${palette.scorePercent}%",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = contentColor,
        )
    }
}

@Composable
private fun PaletteStrip(swatches: List<PaletteSwatch>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .semantics {
                contentDescription = swatches.joinToString(", ") { swatch ->
                    "${swatch.role.displayName}: ${swatch.hex}, ${if (swatch.isOwned) "vorhanden" else "fehlt"}"
                }
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        swatches.forEach { swatch ->
            val color = hexColor(swatch.hex)
            val borderColor = contrastColor(color)
            Box(
                modifier = Modifier
                    .weight(swatch.effectiveMl.coerceAtLeast(80).toFloat())
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
                    .then(
                        if (swatch.isOwned) {
                            Modifier.drawBehind {
                                drawRoundRect(
                                    color = borderColor.copy(alpha = 0.30f),
                                    cornerRadius = CornerRadius(10.dp.toPx()),
                                    style = Stroke(width = 1.dp.toPx()),
                                )
                            }
                        } else {
                            Modifier.drawBehind {
                                drawRoundRect(
                                    color = borderColor,
                                    cornerRadius = CornerRadius(10.dp.toPx()),
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                                    ),
                                )
                            }
                        },
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (!swatch.isOwned) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = borderColor.copy(alpha = 0.90f),
                        modifier = Modifier.padding(6.dp),
                    ) {
                        Text(
                            "FEHLT",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (borderColor == Color.White) Color.Black else Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchLegend(swatch: PaletteSwatch) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ColorMarker(swatch)
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                swatch.role.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(swatch.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(
                listOfNotNull(swatch.productCode, swatch.hex).distinct().joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!swatch.isOwned) {
                Text(
                    listOfNotNull(swatch.lineLabel, swatch.sourceLabel).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (swatch.isOwned) formatMl(swatch.effectiveMl) else "${formatMl(swatch.effectiveMl)} kaufen",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (swatch.isOwned) "${swatch.canCount} Dose${if (swatch.canCount == 1) "" else "n"}" else "fehlt",
                style = MaterialTheme.typography.bodySmall,
                color = if (swatch.isOwned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ColorMarker(swatch: PaletteSwatch) {
    val color = hexColor(swatch.hex)
    val outline = if (swatch.isOwned) MaterialTheme.colorScheme.outline else contrastColor(color)
    Canvas(Modifier.size(28.dp)) {
        drawRoundRect(color = color, cornerRadius = CornerRadius(8.dp.toPx()))
        drawRoundRect(
            color = outline,
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(
                width = if (swatch.isOwned) 1.dp.toPx() else 2.dp.toPx(),
                pathEffect = if (swatch.isOwned) null else PathEffect.dashPathEffect(floatArrayOf(7f, 5f)),
            ),
        )
    }
}

private fun hexColor(hex: String): Color = Color(parseColor(hex))

private fun contrastColor(background: Color): Color {
    val luminance = 0.2126f * background.red + 0.7152f * background.green + 0.0722f * background.blue
    return if (luminance < 0.50f) Color.White else Color.Black
}

private fun formatMl(value: Int): String = NumberFormat.getIntegerInstance(Locale.GERMANY).format(value) + " ml"

private fun formatCandidateCount(value: Long): String = when {
    value >= 999_999_999L -> "999 Mio.+"
    value >= 1_000_000L -> String.format(Locale.GERMANY, "%.1f Mio.+", value / 1_000_000.0)
    value >= 100_000L -> "${NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)}+"
    else -> NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)
}

private fun formatRatio(value: Double): String = String.format(Locale.GERMANY, "%.1f", value)

private fun toneCountHint(toneCount: Int): String = when (toneCount) {
    2 -> "Fill + kontrastierende Outline."
    3 -> "Fill + Outline mit Background, hellem Highlight oder zweiter Fill-Farbe."
    4 -> "Klarer Aufbau mit Highlight, Fill-Fade oder zwei abgestimmten Fill-Flächen."
    5 -> "Kernaufbau oder kreative Variante mit zwei bis drei Fill-Farben."
    6 -> "Outline-System plus hellem Highlight, Fill-Fade oder mehreren Fill-Flächen."
    else -> "Kompletter Aufbau mit Fade/Shadow oder bis zu drei harmonischen Fill-Farben."
}
