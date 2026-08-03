package com.canvault.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.canvault.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.sharedCatalogDataStore by preferencesDataStore(name = "canvault_verified_catalog_v1")

class SharedCatalogRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheKey = stringPreferencesKey("verified_catalog_snapshot")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _snapshot = MutableStateFlow(BundledVerifiedCatalog.snapshot)

    val snapshot: StateFlow<VerifiedCatalogSnapshot> = _snapshot

    init {
        scope.launch {
            restoreCache()
            refresh()
        }
    }

    fun refresh() {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) return
        scope.launch {
            runCatching { downloadCatalog() }
                .onSuccess { downloaded ->
                    if (downloaded.products.isNotEmpty()) {
                        _snapshot.value = downloaded
                        context.sharedCatalogDataStore.edit { preferences ->
                            preferences[cacheKey] = json.encodeToString(downloaded)
                        }
                    }
                }
        }
    }

    private suspend fun restoreCache() {
        val raw = context.sharedCatalogDataStore.data.first()[cacheKey] ?: return
        runCatching { json.decodeFromString<VerifiedCatalogSnapshot>(raw) }
            .getOrNull()
            ?.takeIf(::isTrustworthy)
            ?.let { cached ->
                _snapshot.value = cached.copy(
                    publishedAt = maxOf(cached.publishedAt, BundledVerifiedCatalog.snapshot.publishedAt),
                    products = mergeProducts(BundledVerifiedCatalog.snapshot.products, cached.products),
                    prices = mergePrices(BundledVerifiedCatalog.snapshot.prices, cached.prices),
                    origin = CatalogOrigin.CACHED,
                )
            }
    }

    private fun downloadCatalog(): VerifiedCatalogSnapshot {
        val products = get<List<RemoteVerifiedProduct>>("verified_product_catalog?select=*")
            .mapNotNull(RemoteVerifiedProduct::toDomain)
        val observations = get<List<RemotePriceObservation>>(
            "market_price_observations_public?select=*&currency_code=eq.EUR&active=eq.true",
        ).filter(RemotePriceObservation::isValid)
        val prices = observations
            .groupBy { it.lineId to it.volumeMl }
            .map { (key, observationsForLine) ->
                CatalogPriceAnalysis(key.first, key.second, observationsForLine.map(RemotePriceObservation::toDomain))
            }
        val now = System.currentTimeMillis()

        return VerifiedCatalogSnapshot(
            version = "shared-$now",
            publishedAt = products.maxOfOrNull { it.verifiedAt } ?: BundledVerifiedCatalog.snapshot.publishedAt,
            products = mergeProducts(BundledVerifiedCatalog.snapshot.products, products),
            prices = mergePrices(BundledVerifiedCatalog.snapshot.prices, prices),
            origin = CatalogOrigin.LIVE,
            syncedAtEpochMs = now,
        ).also { require(isTrustworthy(it)) }
    }

    private inline fun <reified T> get(path: String): T {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val connection = URL("$baseUrl/rest/v1/$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 7_000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode in 200..299) { "Catalog HTTP ${connection.responseCode}" }
            json.decodeFromString(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun isTrustworthy(candidate: VerifiedCatalogSnapshot): Boolean =
        candidate.products.isNotEmpty() && candidate.products.all { product ->
            isValidGtin(product.barcode) &&
                catalogBrand(product.brandId) != null &&
                catalogLine(product.lineId) != null &&
                product.sourceUrl.startsWith("https://") &&
                product.verifiedAt.isNotBlank()
        } && candidate.prices.all { analysis ->
            catalogLine(analysis.lineId) != null &&
                analysis.volumeMl > 0 &&
                analysis.observations.isNotEmpty() &&
                analysis.observations.all { it.priceEurCents > 0 && it.sourceUrl.startsWith("https://") }
        }

    private fun mergeProducts(
        bundled: List<VerifiedCatalogProduct>,
        remote: List<VerifiedCatalogProduct>,
    ): List<VerifiedCatalogProduct> = (remote + bundled)
        .distinctBy { barcodeAliasesForCatalog(it.barcode) }

    private fun mergePrices(
        bundled: List<CatalogPriceAnalysis>,
        remote: List<CatalogPriceAnalysis>,
    ): List<CatalogPriceAnalysis> = (remote + bundled)
        .distinctBy { it.lineId }
}

private fun barcodeAliasesForCatalog(value: String): String = when {
    value.length == 12 && value.all(Char::isDigit) -> "0$value"
    else -> value
}

@Serializable
private data class RemoteVerifiedProduct(
    val barcode: String,
    @SerialName("barcode_type") val barcodeType: String,
    @SerialName("brand_slug") val brandId: String,
    @SerialName("can_line_key") val lineId: String,
    @SerialName("color_name") val colorName: String,
    @SerialName("color_code") val colorCode: String? = null,
    @SerialName("hex_approximation") val customHex: String? = null,
    @SerialName("volume_ml") val volumeMl: Int? = null,
    @SerialName("region_code") val regionCode: String? = null,
    @SerialName("source_name") val sourceName: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("verified_at") val verifiedAt: String,
) {
    fun toDomain(): VerifiedCatalogProduct? = VerifiedCatalogProduct(
        barcode = barcode.trim(),
        barcodeType = barcodeType,
        brandId = brandId,
        lineId = lineId,
        colorName = colorName,
        colorCode = colorCode,
        customHex = customHex,
        volumeMl = volumeMl,
        regionCode = regionCode,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        verifiedAt = verifiedAt,
    ).takeIf { product ->
        isValidGtin(product.barcode) &&
            catalogBrand(product.brandId) != null &&
            catalogLine(product.lineId) != null &&
            product.sourceUrl.startsWith("https://")
    }
}

@Serializable
private data class RemotePriceObservation(
    @SerialName("can_line_key") val lineId: String,
    @SerialName("volume_ml") val volumeMl: Int,
    @SerialName("retailer_name") val retailerName: String,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("price_cents") val priceCents: Int,
    @SerialName("observed_at") val observedAt: String,
    @SerialName("tax_included") val taxIncluded: Boolean = true,
    @SerialName("shipping_included") val shippingIncluded: Boolean = false,
    val active: Boolean = true,
) {
    fun toDomain(): CatalogPriceSource = CatalogPriceSource(
        retailerName = retailerName,
        sourceUrl = sourceUrl,
        priceEurCents = priceCents,
        observedAt = observedAt,
        taxIncluded = taxIncluded,
        shippingIncluded = shippingIncluded,
    )

    fun isValid(): Boolean =
        active && catalogLine(lineId) != null && volumeMl > 0 && priceCents > 0 && sourceUrl.startsWith("https://")
}
