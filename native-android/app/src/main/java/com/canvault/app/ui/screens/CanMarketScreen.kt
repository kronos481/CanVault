package com.canvault.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canvault.app.R
import com.canvault.app.data.CatalogBrand
import com.canvault.app.data.CatalogLine
import com.canvault.app.data.CatalogMarket
import com.canvault.app.data.CatalogOrigin
import com.canvault.app.data.CatalogPriceAnalysis
import com.canvault.app.data.SharedCatalogRepository
import com.canvault.app.data.brandName
import com.canvault.app.data.canCatalog
import com.canvault.app.data.catalogDisplayVolumeMl
import com.canvault.app.data.maximumFor
import com.canvault.app.data.minimumFor
import com.canvault.app.data.priceFor
import com.canvault.app.ui.assets.canArtworkRes
import com.canvault.app.ui.components.BrandLogo
import com.canvault.app.ui.sound.LocalCanVaultSounds
import com.canvault.app.ui.sound.UiSoundEffect
import com.canvault.app.ui.sound.soundClick
import com.canvault.app.ui.theme.CanVaultColors
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private data class MarketCatalogItem(
    val brand: CatalogBrand,
    val line: CatalogLine,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanMarketScreen(
    sharedCatalogRepository: SharedCatalogRepository,
    onBack: () -> Unit,
) {
    val catalog by sharedCatalogRepository.snapshot.collectAsStateWithLifecycle()
    val sounds = LocalCanVaultSounds.current
    val backClick = soundClick(onClick = onBack)
    var market by remember { mutableStateOf(CatalogMarket.EUROPE) }
    var marketMenuExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedBrandId by remember { mutableStateOf<String?>(null) }
    var selectedSources by remember { mutableStateOf<CatalogPriceAnalysis?>(null) }
    val allItems = remember {
        canCatalog.flatMap { brand -> brand.lines.map { MarketCatalogItem(brand, it) } }
    }
    val filteredItems = allItems.filter { item ->
        (selectedBrandId == null || item.brand.id == selectedBrandId) &&
            (query.isBlank() || "${item.brand.displayName} ${item.line.displayName}".contains(query, ignoreCase = true))
    }
    val pricedCount = filteredItems.count { item -> catalog.prices.any { it.lineId == item.line.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Can-Markt", fontWeight = FontWeight.Bold)
                        Text(
                            "Alle Dosenlinien & Preischeck",
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
                actions = {
                    Box {
                        OutlinedButton(
                            onClick = {
                                sounds.play(UiSoundEffect.STANDARD)
                                marketMenuExpanded = true
                            },
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) {
                            MarketFlag(market)
                            Text(market.currencyCode, Modifier.padding(start = 7.dp), fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Markt auswählen")
                        }
                        DropdownMenu(
                            expanded = marketMenuExpanded,
                            onDismissRequest = { marketMenuExpanded = false },
                        ) {
                            CatalogMarket.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            MarketFlag(option)
                                            Text("${option.label} · ${option.currencyCode}", Modifier.padding(start = 10.dp))
                                        }
                                    },
                                    onClick = {
                                        sounds.play(UiSoundEffect.STANDARD)
                                        market = option
                                        marketMenuExpanded = false
                                    },
                                    leadingIcon = if (market == option) {
                                        { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CatalogStatusCard(
                    origin = catalog.origin,
                    version = catalog.version,
                    verifiedProducts = catalog.products.size,
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Dose oder Marke suchen") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedBrandId == null,
                            onClick = {
                                sounds.play(UiSoundEffect.STANDARD)
                                selectedBrandId = null
                            },
                            label = { Text("Alle") },
                        )
                    }
                    items(canCatalog, key = { it.id }) { brand ->
                        FilterChip(
                            selected = selectedBrandId == brand.id,
                            onClick = {
                                sounds.play(UiSoundEffect.STANDARD)
                                selectedBrandId = brand.id.takeUnless { selectedBrandId == it }
                            },
                            label = { Text(brand.displayName, maxLines = 1) },
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${filteredItems.size} Dosenlinien",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "$pricedCount mit Preisprüfung · EUR-Basis, ohne Versand",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(filteredItems, key = { it.line.id }) { item ->
                val price = catalog.prices.firstOrNull { it.lineId == item.line.id }
                MarketCanCard(
                    item = item,
                    price = price,
                    market = market,
                    onShowSources = { selectedSources = price },
                )
            }
            item {
                Text(
                    "Preisbasis: öffentlich sichtbare EU-Einzelpreise inkl. ausgewiesener MwSt., ohne Versand und Mengenrabatte. USD, GBP und CHF werden mit den EZB-Referenzkursen vom 31.07.2026 umgerechnet. Händlerpreise können sich jederzeit ändern.",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    selectedSources?.let { price ->
        PriceSourcesDialog(price = price, onDismiss = { selectedSources = null })
    }
}

@Composable
private fun CatalogStatusCard(origin: CatalogOrigin, version: String, verifiedProducts: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CanVaultColors.Mint.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (origin == CatalogOrigin.LIVE) Icons.Rounded.CloudDone else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    if (origin == CatalogOrigin.LIVE) "Gemeinsamer Katalog synchronisiert" else "Verifizierter Offline-Kernkatalog",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$verifiedProducts geprüfte GTIN-Zuordnungen · Version $version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MarketCanCard(
    item: MarketCatalogItem,
    price: CatalogPriceAnalysis?,
    market: CatalogMarket,
    onShowSources: () -> Unit,
) {
    val sourceClick = soundClick(onClick = onShowSources)
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(220)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CatalogCanArtwork(
                lineId = item.line.id,
                brandName = item.brand.displayName,
                lineName = item.line.displayName,
                modifier = Modifier.size(width = 82.dp, height = 118.dp),
            )
            Column(
                modifier = Modifier.padding(start = 14.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                BrandLogo(item.brand.id, Modifier.height(24.dp).fillMaxWidth(0.62f))
                Text(
                    item.line.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${catalogDisplayVolumeMl(item.line.id)} ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (price == null) {
                    Text(
                        "Noch keine belastbaren Preisdaten",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    AnimatedContent(targetState = market, label = "market-price") { selectedMarket ->
                        Column {
                            Text(
                                "${if (price.observations.size >= 2) "Ø" else "Richtwert"} ${formatPrice(price.priceFor(selectedMarket), selectedMarket)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            val sourceLabel = if (price.observations.size == 1) "1 Quelle" else "${price.observations.size} Quellen"
                            Text(
                                if (price.observations.size >= 2) {
                                    "${formatPrice(price.minimumFor(selectedMarket), selectedMarket)}–${formatPrice(price.maximumFor(selectedMarket), selectedMarket)} · $sourceLabel"
                                } else {
                                    "$sourceLabel · EU-Preis umgerechnet"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextButton(
                        onClick = sourceClick,
                        modifier = Modifier.height(48.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Icon(Icons.Rounded.Source, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Quellen ansehen", Modifier.padding(start = 7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCanArtwork(
    lineId: String,
    brandName: String,
    lineName: String,
    modifier: Modifier = Modifier,
) {
    val resource = canArtworkRes(lineId) ?: if (lineId.hashCode() % 2 == 0) R.drawable.can_mint else R.drawable.can_violet
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resource),
            contentDescription = "$brandName $lineName Produktbild",
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun PriceSourcesDialog(price: CatalogPriceAnalysis, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val sounds = LocalCanVaultSounds.current
    val dismissClick = soundClick(onClick = onDismiss)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preisquellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Geprüft am ${price.observations.maxOf { it.observedAt }} · inkl. ausgewiesener MwSt. · ohne Versand",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                price.observations.forEach { source ->
                    Surface(
                        onClick = {
                            sounds.play(UiSoundEffect.STANDARD)
                            uriHandler.openUri(source.sourceUrl)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(source.retailerName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text(formatEuro(source.priceEurCents), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismissClick) { Text("Schließen") } },
    )
}

private fun formatEuro(cents: Int): String = formatPrice(cents, CatalogMarket.EUROPE)

private fun formatPrice(cents: Int, market: CatalogMarket): String {
    val locale = when (market) {
        CatalogMarket.EUROPE -> Locale.GERMANY
        CatalogMarket.UNITED_STATES -> Locale.US
        CatalogMarket.UNITED_KINGDOM -> Locale.UK
        CatalogMarket.SWITZERLAND -> Locale.forLanguageTag("de-CH")
    }
    return NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(market.currencyCode)
    }.format(cents / 100.0)
}

@Composable
private fun MarketFlag(market: CatalogMarket, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(width = 24.dp, height = 16.dp)
            .clip(RoundedCornerShape(2.dp))
            .semantics { contentDescription = "Flagge ${market.label}" },
    ) {
        when (market) {
            CatalogMarket.EUROPE -> {
                drawRect(Color(0xFF173A8F))
                repeat(8) { index ->
                    val angle = Math.toRadians(index * 45.0)
                    drawCircle(
                        color = Color(0xFFFFD84D),
                        radius = size.minDimension * 0.035f,
                        center = Offset(
                            size.width / 2f + kotlin.math.cos(angle).toFloat() * size.width * 0.23f,
                            size.height / 2f + kotlin.math.sin(angle).toFloat() * size.height * 0.28f,
                        ),
                    )
                }
            }
            CatalogMarket.UNITED_STATES -> {
                drawRect(Color.White)
                repeat(7) { stripe ->
                    drawRect(
                        Color(0xFFB22234),
                        topLeft = Offset(0f, stripe * size.height / 7f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height / 14f),
                    )
                }
                drawRect(Color(0xFF3C3B6E), size = androidx.compose.ui.geometry.Size(size.width * 0.45f, size.height * 0.55f))
            }
            CatalogMarket.UNITED_KINGDOM -> {
                drawRect(Color(0xFF173A75))
                drawLine(Color.White, Offset.Zero, Offset(size.width, size.height), strokeWidth = size.height * 0.25f, cap = StrokeCap.Butt)
                drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = size.height * 0.25f, cap = StrokeCap.Butt)
                drawRect(Color.White, topLeft = Offset(0f, size.height * 0.36f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.28f))
                drawRect(Color.White, topLeft = Offset(size.width * 0.39f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height))
                drawRect(Color(0xFFC8102E), topLeft = Offset(0f, size.height * 0.43f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.14f))
                drawRect(Color(0xFFC8102E), topLeft = Offset(size.width * 0.44f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height))
            }
            CatalogMarket.SWITZERLAND -> {
                drawRect(Color(0xFFD52B1E))
                drawRect(Color.White, topLeft = Offset(size.width * 0.25f, size.height * 0.40f), size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.2f))
                drawRect(Color.White, topLeft = Offset(size.width * 0.42f, size.height * 0.2f), size = androidx.compose.ui.geometry.Size(size.width * 0.16f, size.height * 0.6f))
            }
        }
    }
}
