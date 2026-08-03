package com.canvault.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.canvault.app.data.AddCanRequest
import com.canvault.app.data.InventoryRepository
import com.canvault.app.data.OfficialCanColor
import com.canvault.app.data.OfficialCanColorCatalog
import com.canvault.app.data.PurchasePriceSuggestion
import com.canvault.app.data.PurchasePriceSuggestionBasis
import com.canvault.app.data.SharedCatalogRepository
import com.canvault.app.data.canCatalog
import com.canvault.app.data.catalogDisplayVolumeMl
import com.canvault.app.data.catalogLine
import com.canvault.app.data.resolveCanColor
import com.canvault.app.data.resolveCanColorHex
import com.canvault.app.data.suggestPurchasePrice
import com.canvault.app.ui.components.safeCanColor
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class ScanPrefill(
    val brandId: String? = null,
    val lineId: String? = null,
    val colorName: String? = null,
    val colorCode: String? = null,
    val customHex: String? = null,
    val volumeMl: Int? = null,
    val purchasePriceCents: Int? = null,
    val externalBarcode: String? = null,
    val message: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCanScreen(
    repository: InventoryRepository,
    sharedCatalogRepository: SharedCatalogRepository,
    contentPadding: PaddingValues,
    prefill: ScanPrefill?,
    onPrefillConsumed: () -> Unit,
    onScan: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val sounds = LocalCanVaultSounds.current
    val scope = rememberCoroutineScope()
    val verifiedCatalog by sharedCatalogRepository.snapshot.collectAsStateWithLifecycle()
    var brandId by rememberSaveable { mutableStateOf(canCatalog.first().id) }
    var lineId by rememberSaveable { mutableStateOf(canCatalog.first().lines.first().id) }
    var colorName by rememberSaveable { mutableStateOf("") }
    var colorCode by rememberSaveable { mutableStateOf("") }
    var customHex by rememberSaveable { mutableStateOf("") }
    var volume by rememberSaveable { mutableStateOf("400") }
    var price by rememberSaveable { mutableStateOf("") }
    var priceManuallyEdited by rememberSaveable { mutableStateOf(false) }
    var fillPercent by rememberSaveable { mutableIntStateOf(100) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var externalBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var scanMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val selectedBrand = canCatalog.firstOrNull { it.id == brandId } ?: canCatalog.first()
    val selectedLine = selectedBrand.lines.firstOrNull { it.id == lineId } ?: selectedBrand.lines.first()
    val officialLineColors = remember(lineId) { OfficialCanColorCatalog.colorsForLine(lineId) }
    val officialMatch = remember(lineId, colorName, colorCode) {
        OfficialCanColorCatalog.find(lineId, colorName, colorCode)
    }
    val resolvedColor = remember(lineId, colorName, colorCode, customHex) {
        resolveCanColor(customHex, colorCode, colorName, lineId)
    }
    val selectedVolumeMl = volume.toIntOrNull()
    val priceSuggestion = remember(verifiedCatalog, lineId, selectedVolumeMl) {
        selectedVolumeMl?.let { verifiedCatalog.suggestPurchasePrice(lineId, it) }
    }
    val hexValid = customHex.isBlank() || Regex("^#[0-9A-Fa-f]{6}$").matches(customHex)
    val formValid = colorName.isNotBlank() && hexValid && !saving
    val canAccent = safeCanColor(resolvedColor?.hex ?: resolveCanColorHex(customHex, colorCode, colorName, lineId))

    LaunchedEffect(prefill) {
        if (prefill != null) {
            prefill.brandId?.takeIf { id -> canCatalog.any { it.id == id } }?.let { brandId = it }
            prefill.lineId?.let { lineId = it }
            prefill.colorName?.let { colorName = it }
            prefill.colorCode?.let { colorCode = it }
            prefill.customHex?.let { customHex = it }
            prefill.volumeMl?.let { volume = it.toString() }
            prefill.purchasePriceCents?.let {
                price = formatPriceInput(it)
                priceManuallyEdited = true
            } ?: run {
                priceManuallyEdited = false
            }
            externalBarcode = prefill.externalBarcode
            scanMessage = prefill.message
            onPrefillConsumed()
        }
    }

    LaunchedEffect(brandId) {
        val lines = canCatalog.first { it.id == brandId }.lines
        if (lines.none { it.id == lineId }) {
            val firstLine = lines.first()
            lineId = firstLine.id
            volume = catalogDisplayVolumeMl(firstLine.id).toString()
            priceManuallyEdited = false
            colorName = firstLine.defaultColorName.orEmpty()
            colorCode = firstLine.defaultColorCode.orEmpty()
            customHex = firstLine.defaultColorHex.orEmpty()
        }
    }

    LaunchedEffect(lineId, selectedVolumeMl, priceSuggestion?.priceEurCents, priceManuallyEdited) {
        if (!priceManuallyEdited) {
            price = priceSuggestion?.priceEurCents?.let(::formatPriceInput).orEmpty()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                photoPath = withContext(Dispatchers.IO) { copyPhoto(context, uri) }
            }
        }
    }

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
            Column(Modifier.weight(1f)) {
                Text("Dose hinzufügen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Daten prüfen und lokal speichern", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onScan) {
                Icon(Icons.Rounded.DocumentScanner, contentDescription = null)
                Text("Barcode", Modifier.padding(start = 8.dp))
            }
        }

        AnimatedVisibility(scanMessage != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Text(scanMessage.orEmpty(), Modifier.padding(start = 12.dp))
                }
            }
        }

        AnimatedVisibility(externalBarcode != null) {
            OutlinedTextField(
                value = externalBarcode.orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text("Produkt-Barcode") },
                leadingIcon = { Icon(Icons.Rounded.DocumentScanner, contentDescription = null) },
                supportingText = { Text("Wird mit diesem Produkt lokal gespeichert und beim nächsten Scan erkannt.") },
                singleLine = true,
            )
        }

        Text("Produkt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        CatalogDropdown(
            label = "Marke",
            selected = selectedBrand.displayName,
            options = canCatalog.map { it.id to it.displayName },
            onSelect = {
                brandId = it
                priceManuallyEdited = false
            },
        )
        CatalogDropdown(
            label = "Dosenlinie",
            selected = selectedLine.displayName,
            options = selectedBrand.lines.map { it.id to it.displayName },
            onSelect = { selectedId ->
                lineId = selectedId
                priceManuallyEdited = false
                catalogLine(selectedId)?.let { line ->
                    volume = catalogDisplayVolumeMl(selectedId).toString()
                    colorName = line.defaultColorName.orEmpty()
                    colorCode = line.defaultColorCode.orEmpty()
                    customHex = line.defaultColorHex.orEmpty()
                }
            },
        )

        Text("Farbe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OfficialColorNameField(
            value = colorName,
            lineId = lineId,
            officialMatch = officialMatch,
            officialColorCount = officialLineColors.size,
            onValueChange = { colorName = it },
            onSelect = { color ->
                colorName = color.colorName
                colorCode = color.colorCode ?: color.productCode.orEmpty()
                customHex = ""
            },
        )
        OutlinedTextField(
            value = colorCode,
            onValueChange = { colorCode = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Farb- oder Produktcode") },
            singleLine = true,
        )
        OutlinedTextField(
            value = customHex,
            onValueChange = { customHex = it.take(7) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Eigener HEX-Wert (optional)") },
            placeholder = { Text("#58E4C2") },
            singleLine = true,
            isError = !hexValid,
            supportingText = {
                Text(
                    when {
                        !hexValid -> "Format: #RRGGBB"
                        officialMatch != null -> "Der veröffentlichte Herstellerwert ${officialMatch.hex} hat Vorrang."
                        else -> "Nur verwenden, wenn der Hersteller keinen digitalen Farbwert veröffentlicht."
                    },
                )
            },
            trailingIcon = {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(canAccent),
                )
            },
        )

        Text("Bestand", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Füllstand", style = MaterialTheme.typography.labelLarge)
                        Text(
                            when (fillPercent) {
                                0 -> "Leer"
                                100 -> "Voll"
                                else -> "Geöffnet"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "$fillPercent %",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = canAccent,
                    )
                }
                Slider(
                    value = fillPercent.toFloat(),
                    onValueChange = { fillPercent = ((it / 5f).roundToInt() * 5).coerceIn(0, 100) },
                    onValueChangeFinished = { sounds.play(UiSoundEffect.STANDARD) },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = canAccent,
                        activeTrackColor = canAccent,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0 %", style = MaterialTheme.typography.labelSmall)
                    Text("in 5-%-Schritten", style = MaterialTheme.typography.labelSmall)
                    Text("100 %", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = volume,
                onValueChange = { volume = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.weight(1f),
                label = { Text("Volumen (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = price,
                onValueChange = {
                    price = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(8)
                    priceManuallyEdited = true
                },
                modifier = Modifier.weight(1f),
                label = { Text("Preis (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    priceSuggestion?.let { suggestion ->
                        Text(priceSuggestionText(suggestion, priceManuallyEdited))
                    }
                },
                singleLine = true,
            )
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Menge", style = MaterialTheme.typography.labelLarge)
                    Text("$quantity Dosen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        quantity = (quantity - 1).coerceAtLeast(1)
                    }, enabled = quantity > 1) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Menge verringern")
                    }
                    Text(quantity.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        quantity = (quantity + 1).coerceAtMost(99)
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Menge erhöhen")
                    }
                }
            }
        }

        Text("Foto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (photoPath != null) {
            AsyncImage(
                model = photoPath,
                contentDescription = "Ausgewähltes Dosenfoto",
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
        }
        OutlinedButton(
            onClick = {
                sounds.play(UiSoundEffect.STANDARD)
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
            Text(if (photoPath == null) "Foto auswählen" else "Foto ändern", Modifier.padding(start = 8.dp))
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                saving = true
                scope.launch {
                    val verifiedColor = OfficialCanColorCatalog.find(lineId, colorName, colorCode)
                    repository.add(
                        AddCanRequest(
                            brandId = brandId,
                            canLineId = lineId,
                            colorName = verifiedColor?.colorName ?: colorName.trim(),
                            colorCode = verifiedColor?.colorCode ?: verifiedColor?.productCode ?: colorCode.ifBlank { null },
                            customHex = verifiedColor?.hex ?: customHex.ifBlank { null },
                            volumeMl = volume.toIntOrNull(),
                            fillPercent = fillPercent,
                            quantity = quantity,
                            purchasePriceCents = price.replace(',', '.').toDoubleOrNull()?.times(100)?.toInt(),
                            photoPath = photoPath,
                            externalBarcode = externalBarcode,
                        ),
                    )
                    saving = false
                    onSaved()
                }
            },
            enabled = formValid,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(if (saving) "Wird gespeichert …" else "$quantity ${if (quantity == 1) "Dose" else "Dosen"} speichern")
        }
    }
}

private fun formatPriceInput(cents: Int): String =
    String.format(Locale.GERMANY, "%.2f", cents / 100.0)

private fun priceSuggestionText(
    suggestion: PurchasePriceSuggestion,
    manuallyEdited: Boolean,
): String {
    val basis = when (suggestion.basis) {
        PurchasePriceSuggestionBasis.EXACT_LINE -> if (suggestion.observationCount >= 2) {
            "Ø aus ${suggestion.observationCount} Händlerpreisen"
        } else {
            "Richtwert aus 1 Händlerpreis"
        }
        PurchasePriceSuggestionBasis.SAME_LINE_SCALED ->
            "aus derselben Linie auf ${suggestion.volumeMl} ml hochgerechnet"
        PurchasePriceSuggestionBasis.BRAND_FORMAT ->
            "Ø der Marke für ${suggestion.volumeMl} ml"
        PurchasePriceSuggestionBasis.FORMAT ->
            "Ø vergleichbarer ${suggestion.volumeMl}-ml-Dosen"
        PurchasePriceSuggestionBasis.FORMAT_SCALED ->
            "Format-Richtwert für ${suggestion.volumeMl} ml"
    }
    val date = suggestion.latestObservedAt?.let { " · Stand $it" }.orEmpty()
    return if (manuallyEdited) {
        "Eigene Eingabe · Vorschlag ${formatPriceInput(suggestion.priceEurCents)} € ($basis)"
    } else {
        "Automatisch: $basis$date · überschreibbar"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficialColorNameField(
    value: String,
    lineId: String,
    officialMatch: OfficialCanColor?,
    officialColorCount: Int,
    onValueChange: (String) -> Unit,
    onSelect: (OfficialCanColor) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val sounds = LocalCanVaultSounds.current
    val suggestions = remember(lineId, value) {
        OfficialCanColorCatalog.search(lineId, value, limit = 8)
    }
    val source = officialMatch?.let(OfficialCanColorCatalog::sourceFor)

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it && suggestions.isNotEmpty() },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
            label = { Text("Farbname *") },
            singleLine = true,
            supportingText = {
                Text(
                    when {
                        officialMatch != null -> "${officialMatch.hex} · ${source?.label.orEmpty()} · digital verifiziert"
                        officialColorCount > 0 -> "$officialColorCount veröffentlichte Farben verfügbar – Name vollständig eingeben"
                        else -> "Für diese Linie ist kein maschinenlesbarer Hersteller-HEX veröffentlicht."
                    },
                )
            },
            trailingIcon = {
                if (officialMatch != null) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(safeCanColor(officialMatch.hex)),
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { color ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(color.colorName, fontWeight = FontWeight.Medium)
                            Text(
                                listOfNotNull(color.colorCode, color.productCode, color.hex).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(safeCanColor(color.hex)),
                        )
                    },
                    onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        onSelect(color)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val sounds = LocalCanVaultSounds.current
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
        sounds.play(UiSoundEffect.STANDARD)
        expanded = !expanded
    }) {
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
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        sounds.play(UiSoundEffect.STANDARD)
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun copyPhoto(context: Context, uri: Uri): String {
    val directory = File(context.filesDir, "can_photos").apply { mkdirs() }
    val destination = File(directory, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Foto konnte nicht geöffnet werden." }
        destination.outputStream().use(input::copyTo)
    }
    return destination.toURI().toString()
}
