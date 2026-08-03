package com.canvault.app.data

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class CatalogOrigin {
    BUNDLED,
    CACHED,
    LIVE,
}

@Serializable
data class VerifiedCatalogProduct(
    val barcode: String,
    val barcodeType: String = "EAN_13",
    val brandId: String,
    val lineId: String,
    val colorName: String,
    val colorCode: String? = null,
    val customHex: String? = null,
    val volumeMl: Int? = null,
    val regionCode: String? = null,
    val sourceName: String,
    val sourceUrl: String,
    val verifiedAt: String,
)

@Serializable
data class CatalogPriceSource(
    val retailerName: String,
    val sourceUrl: String,
    val priceEurCents: Int,
    val observedAt: String,
    val taxIncluded: Boolean = true,
    val shippingIncluded: Boolean = false,
)

@Serializable
data class CatalogPriceAnalysis(
    val lineId: String,
    val volumeMl: Int,
    val observations: List<CatalogPriceSource>,
) {
    val averageEurCents: Int
        get() = observations.map { it.priceEurCents }.average().roundToInt()

    val minimumEurCents: Int
        get() = observations.minOf { it.priceEurCents }

    val maximumEurCents: Int
        get() = observations.maxOf { it.priceEurCents }
}

@Serializable
data class VerifiedCatalogSnapshot(
    val version: String,
    val publishedAt: String,
    val products: List<VerifiedCatalogProduct>,
    val prices: List<CatalogPriceAnalysis>,
    val origin: CatalogOrigin = CatalogOrigin.BUNDLED,
    val syncedAtEpochMs: Long? = null,
)

enum class CatalogMarket(
    val marketCode: String,
    val label: String,
    val currencyCode: String,
    val euroRate: Double,
) {
    EUROPE("EU", "Europa", "EUR", 1.0),
    UNITED_STATES("US", "USA", "USD", 1.1485),
    UNITED_KINGDOM("GB", "UK", "GBP", 0.85573),
    SWITZERLAND("CH", "Schweiz", "CHF", 0.9304),
}

fun CatalogPriceAnalysis.priceFor(market: CatalogMarket): Int =
    (averageEurCents * market.euroRate).roundToInt()

fun CatalogPriceAnalysis.minimumFor(market: CatalogMarket): Int =
    (minimumEurCents * market.euroRate).roundToInt()

fun CatalogPriceAnalysis.maximumFor(market: CatalogMarket): Int =
    (maximumEurCents * market.euroRate).roundToInt()

enum class PurchasePriceSuggestionBasis {
    EXACT_LINE,
    SAME_LINE_SCALED,
    BRAND_FORMAT,
    FORMAT,
    FORMAT_SCALED,
}

data class PurchasePriceSuggestion(
    val priceEurCents: Int,
    val volumeMl: Int,
    val basis: PurchasePriceSuggestionBasis,
    val lineCount: Int,
    val observationCount: Int,
    val latestObservedAt: String?,
)

/**
 * Returns the strongest available EUR retail reference for an add-can form.
 * Exact line/volume observations win. Fallbacks are averages of line averages,
 * so a line with many retailer observations cannot dominate the result.
 */
fun VerifiedCatalogSnapshot.suggestPurchasePrice(
    lineId: String,
    volumeMl: Int,
): PurchasePriceSuggestion? {
    if (volumeMl <= 0) return null

    fun suggestion(
        analyses: List<CatalogPriceAnalysis>,
        basis: PurchasePriceSuggestionBasis,
        cents: Int = analyses.map(CatalogPriceAnalysis::averageEurCents).average().roundToInt(),
    ) = PurchasePriceSuggestion(
        priceEurCents = cents.coerceAtLeast(1),
        volumeMl = volumeMl,
        basis = basis,
        lineCount = analyses.size,
        observationCount = analyses.sumOf { it.observations.size },
        latestObservedAt = analyses.flatMap { it.observations }.maxOfOrNull { it.observedAt },
    )

    prices.firstOrNull { it.lineId == lineId && it.volumeMl == volumeMl }?.let { exact ->
        return suggestion(listOf(exact), PurchasePriceSuggestionBasis.EXACT_LINE)
    }

    prices.filter { it.lineId == lineId }
        .minByOrNull { kotlin.math.abs(it.volumeMl - volumeMl) }
        ?.let { sameLine ->
            val scaledCents = (sameLine.averageEurCents * volumeMl.toDouble() / sameLine.volumeMl).roundToInt()
            return suggestion(listOf(sameLine), PurchasePriceSuggestionBasis.SAME_LINE_SCALED, scaledCents)
        }

    val brandId = lineId.substringBefore(':')
    prices.filter { it.lineId.substringBefore(':') == brandId && it.volumeMl == volumeMl }
        .takeIf(List<CatalogPriceAnalysis>::isNotEmpty)
        ?.let { sameBrand ->
            return suggestion(sameBrand, PurchasePriceSuggestionBasis.BRAND_FORMAT)
        }

    prices.filter { it.volumeMl == volumeMl }
        .takeIf(List<CatalogPriceAnalysis>::isNotEmpty)
        ?.let { sameFormat ->
            return suggestion(sameFormat, PurchasePriceSuggestionBasis.FORMAT)
        }

    return prices.takeIf(List<CatalogPriceAnalysis>::isNotEmpty)?.let { allPrices ->
        val averageCentsPerMl = allPrices.map { it.averageEurCents.toDouble() / it.volumeMl }.average()
        suggestion(
            analyses = allPrices,
            basis = PurchasePriceSuggestionBasis.FORMAT_SCALED,
            cents = (averageCentsPerMl * volumeMl).roundToInt(),
        )
    }
}

fun isValidGtin(rawValue: String): Boolean {
    val value = rawValue.trim()
    if (value.length !in setOf(8, 12, 13, 14) || value.any { !it.isDigit() }) return false

    val sum = value.dropLast(1)
        .reversed()
        .mapIndexed { index, character ->
            character.digitToInt() * if (index % 2 == 0) 3 else 1
        }
        .sum()
    val expectedCheckDigit = (10 - sum % 10) % 10
    return expectedCheckDigit == value.last().digitToInt()
}

fun VerifiedCatalogSnapshot.findVerifiedProduct(rawValue: String): VerifiedCatalogProduct? =
    products.firstOrNull { product -> barcodesEquivalent(product.barcode, rawValue) }

fun catalogDisplayVolumeMl(lineId: String): Int =
    BundledVerifiedCatalog.snapshot.prices.firstOrNull { it.lineId == lineId }?.volumeMl
        ?: catalogLine(lineId)?.defaultVolumeMl
        ?: when (lineId) {
            "mtn-montana-colors:mtn-mega" -> 600
            "mtn-montana-colors:mtn-alien" -> 250
            "montana-cans:montana-tarblack" -> 500
            "montana-cans:montana-blackout-tarblack" -> 400
            "montana-cans:montana-ultra-wide" -> 750
            "molotow-belton:molotow-burner" -> 600
            "molotow-belton:molotow-coversall" -> 600
            "loop-colors:loop-asphalt" -> 600
            "dope:dope-action" -> 600
            "krink:krink-k-750" -> 750
            else -> 400
        }

object BundledVerifiedCatalog {
    private const val verifiedDate = "2026-08-03"

    val snapshot = VerifiedCatalogSnapshot(
        version = "2026.08.03-v2",
        publishedAt = verifiedDate,
        products = listOf(
            product("4015962368101", "molotow-belton", "molotow-belton:burner-chrome-600-ml", "Metallic Chrome", "940397", "#B7BDC3", 600, "Van Beek Art Supplies", "https://www.vanbeekart.nl/p/molotow-burner-600ml-chrome/82318/"),
            product("4015962368286", "molotow-belton", "molotow-belton:burner-gold-600-ml", "Metallic Gold", "940499", "#C29A45", 600, "Blue Label Shops", "https://www.graffitidirect.nl/p/molotow-burner-600ml-gold/60137/"),
            product("4015962368293", "molotow-belton", "molotow-belton:burner-copper-600-ml", "Metallic Copper", "940500", "#B66A45", 600, "Molotow España", "https://www.molotow.es/spray-burner/spray-efecto-metalizado-molotow-burner-cobre-600ml-4015962368293-1722.html"),
            product("4015962369429", "molotow-belton", "molotow-belton:burner-black-600-ml", "Black", "940398", "#090909", 600, "DynaTech", "https://www.dynatech.de/molotow-spruehdose-600-burner-black-600-ml-matt-farbe-schwarz"),
            product("4048500264368", "montana-cans", "montana-cans:montana-black", "Black", "BLK 9001", "#111111", 400, "Allegro", "https://allegro.cz/nabidka/montana-black-400-ml-blk-9001-black-11174154371"),
            product("4048500321573", "montana-cans", "montana-cans:montana-black", "Storm", null, null, 400, "Store-HD", "https://www.store-hd.de/Montana-Black-Storm-400ml"),
            product("4048500285783", "montana-cans", "montana-cans:montana-gold", "Shock Black", "S9000", "#101010", 400, "Architekturbedarf", "https://www.architekturbedarf.de/paints/spray-paints/montana-gold-400-ml/montana-gold-s9000-shock-black"),
            product("8427744411367", "mtn-montana-colors", "mtn-montana-colors:mtn-94", "Black", "RV-9011", "#111111", 400, "Allegro", "https://allegro.hu/termek/matt-fekete-spray-festek-montana-400-ml-c8aeb0d8-7c03-453d-ac4e-e65c8d403b23"),
            product("7909547444922", "mtn-montana-colors", "mtn-montana-colors:mtn-hardcore", "Black", null, "#111111", 400, "Pinheiro Tintas", "https://www.pinheirotintas.com.br/tinta-spray-brilhante-mtn-hardcore-preto-400ml-montana/p", "BR"),
            product("4250397612942", "flame", "flame:flame-blue", "Deep Black", "FB-904", "#090909", 400, "Allegro", "https://allegro.pl/produkt/flame-blue-fb-904-deep-black-400ml-cb2288c1-462b-437a-961f-57d6c19a0c3e"),
            product("8051277870546", "kobra", "kobra:kobra-hp", "Satin Black", "006", "#111111", 400, "eBay UK", "https://www.ebay.co.uk/itm/285629279687"),
            product("5901687941004", "dope", "dope:dope-classic", "Fresh Color", null, null, 400, "Spreje Plzeň", "https://www.sprejeplzen.cz/dope-classic-400ml/"),
            product("5901687941585", "dope", "dope:dope-classic", "White", null, "#F4F4F1", 400, "Spreje Plzeň", "https://www.sprejeplzen.cz/dope-classic-400ml/"),
            product("5901687941592", "dope", "dope:dope-classic", "Black", "D-300", "#090909", 400, "Tulandia", "https://tulandia.pl/farba-w-sprayu-graffiti-400-ml-czarna-dope-classic-p-10813.html"),
            product("5901687941608", "dope", "dope:dope-classic", "Chrome", null, "#B8BEC5", 400, "Spreje Plzeň", "https://www.sprejeplzen.cz/dope-classic-400ml/"),
            product("5901687941615", "dope", "dope:dope-classic", "Gold", null, "#C19A47", 400, "Spreje Plzeň", "https://www.sprejeplzen.cz/dope-classic-400ml/"),
            product("4255883101658", "double-a", "double-a:double-a", "Damagers Red", "DA-380", "#B9202C", 400, "Double A", "https://doublea-spraypaint.com/products/double-a-spraypaint-400ml-special-edition-damagers"),
            product("8436548872625", "nbq", "nbq:nbq-fast", "Waste Green", null, null, 400, "bol", "https://www.bol.com/nl/nl/p/nbq-fast-spray-paint-400ml-matte-afwerking-hogedruk-mat-sneldrogend/9300000231509022/"),
            product("8427744143657", "krink", "krink:krink-k-750", "Black", null, "#090909", 750, "Nicolaas Verf", "https://www.nicolaasverf.nl/product/mtn-krink-k-750/"),
        ).filter { isValidGtin(it.barcode) },
        prices = listOf(
            price("mtn-montana-colors:mtn-94", 400,
                source("MTN Shop", "https://www.mtn-shop.de/mtn-94-ex0140126", 510),
                source("BETTERRUN", "https://www.betterrun.shop/", 480),
                source("idealo", "https://www.idealo.de/preisvergleich/OffersOfProduct/3734100_-montana-colors-mtn-94-spruehfarbe-400-ml-verschiedene-farben-montanacans.html", 595)),
            price("mtn-montana-colors:mtn-hardcore", 400,
                source("Psychic Shop", "https://www.psychic-shop.de/MTN-Hardcore-400ml/SW10066.117", 430),
                source("BETTERRUN", "https://www.betterrun.shop/spruehdosen/action-cans/mtn-cans-hardcore-400ml-139-farben", 450)),
            price("mtn-montana-colors:mtn-vice", 400,
                source("Graffitibox", "https://graffitibox.de/spruehdosen/mtn-vice/", 410),
                source("Graffitishop Kiel", "https://graffitishopkiel.de/Graffiti-Spraydosen/MTN-Spraydosen/MTN-VICE%3A%3A%3A1_5_75.html", 425)),
            price("mtn-montana-colors:mtn-water-based-400", 400,
                source("MTN Shop", "https://www.mtn-shop.de/mtn-water-based-400", 855),
                source("Pintaya", "https://pintaya.com/shop/mtn-water-based-400-19", 775)),
            price("mtn-montana-colors:mtn-mega", 600,
                source("Graffitibox", "https://graffitibox.de/spruehdosen/mtn-mega/", 690),
                source("BETTERRUN", "https://www.betterrun.shop/en/spray-cans/action-spray-cans/mtn-cans-mega-colors-600ml-20-colors", 675),
                source("Spectrum", "https://spectrumstore.com/en-eu/collections/montana-mtn-mega", 620)),
            price("mtn-montana-colors:mtn-alien", 250,
                source("Impulse Innovation", "https://shop.impulse-innovation.de/Spruehlack-MTN-ALIEN-Black-White-250ml", 755),
                source("Nicolaas Verf", "https://www.nicolaasverf.nl/product/mtn-alien-250ml/", 407),
                source("Art & Colour", "https://www.artcolour.gr/en/shop/craft-materials/graffiti-en/graffiti-spraycans/spray-paint-montana-alien-250-ml/", 400),
                source("Flow Control", "https://flow-control.at/eshop/graffiti/mtn/spruehdosen-mtn/mtn-spruehdosen/mtn-alien-250ml-2/", 335)),
            price("montana-cans:montana-black", 400,
                source("Store-HD", "https://www.store-hd.de/Montana-Black-Storm-400ml", 421),
                source("BETTERRUN", "https://www.betterrun.shop/", 470)),
            price("montana-cans:montana-gold", 400,
                source("Architekturbedarf", "https://www.architekturbedarf.de/paints/spray-paints/montana-gold-400-ml/montana-gold-s9000-shock-black", 525),
                source("Dekkender Paints", "https://dekkenderpaints.nl/product/s9000-shock-black-400ml-285783/", 516),
                source("BETTERRUN", "https://www.betterrun.shop/", 550)),
            price("montana-cans:montana-white", 400,
                source("Graffitilager", "https://graffitilager.de/en/Spray-cans/Montana-Cans/White-series/", 455),
                source("AGRABAH", "https://agrabah.de/produkt/montana-white-400ml/", 490)),
            price("montana-cans:montana-tarblack", 500,
                source("Dedicated Store", "https://www.dedicated-store.com/startseite/902-montana-tarblack-500ml.html", 460),
                source("Graffitilager", "https://graffitilager.de/montana-tarblack-500ml", 555),
                source("BETTERRUN", "https://www.betterrun.shop/action-cans/", 570)),
            price("montana-cans:montana-blackout-tarblack", 400,
                source("BETTERRUN", "https://www.betterrun.shop/spruehdosen/action-cans/montana-cans-blackout-400ml-schwarz", 495),
                source("OVERKILL", "https://www.overkillshop.com/products/montana-blackout-400-ml-mon401435", 470),
                source("Graffiti Shop Berlin", "https://www.graffitishop-berlin.de/montana-blackout-tarblack-400ml-spruehdose.html", 480)),
            price("montana-cans:montana-ultra-wide", 750,
                source("Graffitilager", "https://graffitilager.de/en/Spray-cans/Montana-Cans/Ultrawide/", 755),
                source("Ultra Wide Barcelona", "https://ultrawide.es/shop/montana-cans/montana-ultra-wide-750ml-spray-graffiti/", 795),
                source("BETTERRUN", "https://www.betterrun.shop/en/montana-bombing-cans/", 850)),
            price("molotow-belton:molotow-premium", 400,
                source("BETTERRUN", "https://www.betterrun.shop/", 495)),
            price("molotow-belton:burner-chrome-600-ml", 600,
                source("Molotow France", "https://molotow.fr/bombe-burner-chrome-600ml.html", 780),
                source("Van Beek", "https://www.vanbeekart.nl/p/molotow-burner-600ml-chrome/82318/", 835),
                source("Molotow España", "https://www.molotow.es/spray-burner/spray-de-pintura-molotow-burner-600ml-4015962368101-22.html", 600)),
            price("molotow-belton:burner-gold-600-ml", 600,
                source("Blue Label Shops", "https://www.graffitidirect.nl/p/molotow-burner-600ml-gold/60137/", 877)),
            price("molotow-belton:burner-copper-600-ml", 600,
                source("Molotow España", "https://www.molotow.es/spray-burner/spray-efecto-metalizado-molotow-burner-cobre-600ml-4015962368293-1722.html", 650),
                source("Molotow France", "https://molotow.fr/bombe-de-peinture-graffiti-metallisee-molotow-burner-cuivre-600ml.html", 790)),
            price("molotow-belton:burner-black-600-ml", 600,
                source("Molotow France", "https://molotow.fr/bombe-de-peinture-graffiti-noire-molotow-burner-black-600ml.html", 790),
                source("Molotow Slovakia", "https://en.molotow.sk/burnertm-black-600-ml.html", 660),
                source("DynaTech", "https://www.dynatech.de/molotow-spruehdose-600-burner-black-600-ml-matt-farbe-schwarz", 710),
                source("BETTERRUN", "https://www.betterrun.shop/en/spray-cans/action-spray-cans/molotov-burner-black-600ml-black", 625),
                source("OVERKILL", "https://www.overkillshop.com/products/molotow-burner-black-600-ml-940398", 550)),
            price("molotow-belton:molotow-coversall", 400,
                source("Van Beek", "https://www.vanbeekart.nl/p/molotow-burner-600ml-chrome/82318/", 886)),
            price("loop-colors:loop-400-ml", 400,
                source("Loopcolors Germany", "https://www.loopcolors-germany.de/cans/loop-400/", 435)),
            price("loop-colors:loop-asphalt", 400,
                source("Loopcolors Germany", "https://www.loopcolors-germany.de/cans/loop-400/", 465)),
            price("flame:flame-blue", 400,
                source("Molotow Shop", "https://shop.molotow.com/produkt/flame-blue/", 445)),
            price("flame:flame-orange", 400,
                source("BETTERRUN", "https://www.betterrun.shop/", 400)),
            price("kobra:kobra-hp", 400,
                source("BETTERRUN", "https://www.betterrun.shop/", 395)),
            price("kobra:kobra-lp", 400,
                source("BETTERRUN", "https://www.betterrun.shop/", 410)),
            price("nbq:nbq-fast", 400,
                source("Writers Madrid", "https://www.writersmadrid.es/es/nbq-pro-spray-paint/2480-nbq-fast-400ml.html", 395),
                source("Graffitibox", "https://graffitibox.de/spruehdosen/schwarz/10815/nbq-new-fast-pro-spraypaint-black-400ml", 390)),
            price("nbq:nbq-slow", 400,
                source("Dedicated Store", "https://www.dedicated-store.com/startseite/1886-nbq-slow-400ml.html", 430),
                source("Allcity", "https://www.allcity.fr/nbq-slow-400ml.html", 425),
                source("Graffitibox", "https://graffitibox.de/spruehdosen/standard/10909/nbq-new-slow-pro-spraypaint-400ml", 460)),
            price("dope:dope-action", 600,
                source("BETTERRUN", "https://www.betterrun.shop/dope-cans-spruehdosen/", 450)),
            price("dope:dope-classic", 400,
                source("BETTERRUN", "https://www.betterrun.shop/", 350)),
            price("clash:clash", 400,
                source("Clash Paint", "https://www.clashpaint.com/it/spray/clash-400-ml", 400),
                source("Graffitibox", "https://graffitibox.de/spruehdosen/standard/158/clash-paint-400ml", 450)),
            price("double-a:double-a", 400,
                source("CLRZ", "https://www.clrz.de/DoubleA", 390)),
            price("krink:krink-k-750", 750,
                source("Nicolaas Verf", "https://www.nicolaasverf.nl/product/mtn-krink-k-750/", 944)),
        ),
    )

    private fun product(
        barcode: String,
        brandId: String,
        lineId: String,
        colorName: String,
        colorCode: String?,
        customHex: String?,
        volumeMl: Int,
        sourceName: String,
        sourceUrl: String,
        regionCode: String? = "EU",
    ) = VerifiedCatalogProduct(
        barcode = barcode,
        brandId = brandId,
        lineId = lineId,
        colorName = colorName,
        colorCode = colorCode,
        customHex = customHex,
        volumeMl = volumeMl,
        regionCode = regionCode,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        verifiedAt = verifiedDate,
    )

    private fun source(name: String, url: String, priceCents: Int) = CatalogPriceSource(
        retailerName = name,
        sourceUrl = url,
        priceEurCents = priceCents,
        observedAt = verifiedDate,
    )

    private fun price(lineId: String, volumeMl: Int, vararg sources: CatalogPriceSource) =
        CatalogPriceAnalysis(lineId, volumeMl, sources.toList())
}
