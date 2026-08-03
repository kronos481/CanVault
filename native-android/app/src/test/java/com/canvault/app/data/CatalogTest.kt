package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun molotowBurner600SeriesHasVerifiedDefaults() {
        val expected = mapOf(
            "molotow-belton:burner-chrome-600-ml" to "940397E",
            "molotow-belton:burner-gold-600-ml" to "940499",
            "molotow-belton:burner-copper-600-ml" to "940500",
            "molotow-belton:burner-black-600-ml" to "940398",
        )

        expected.forEach { (lineId, productCode) ->
            val line = catalogLine(lineId)
            assertEquals(600, line?.defaultVolumeMl)
            assertEquals(productCode, line?.defaultColorCode)
        }
    }

    @Test
    fun bundledBarcodesHaveValidCheckDigitsAndSources() {
        val products = BundledVerifiedCatalog.snapshot.products

        assertTrue(products.size >= 15)
        assertTrue(products.all { isValidGtin(it.barcode) })
        assertTrue(products.all { it.sourceUrl.startsWith("https://") })
        assertEquals(products.size, products.map { it.barcode }.distinct().size)
    }

    @Test
    fun priceAnalysesAreTraceableAndInternallyConsistent() {
        assertEquals(38, canCatalog.sumOf { it.lines.size })
        assertEquals(31, BundledVerifiedCatalog.snapshot.prices.size)
        assertEquals(62, BundledVerifiedCatalog.snapshot.prices.sumOf { it.observations.size })

        BundledVerifiedCatalog.snapshot.prices.forEach { analysis ->
            assertTrue(analysis.observations.isNotEmpty())
            assertTrue(analysis.observations.all { it.priceEurCents > 0 })
            assertTrue(analysis.minimumEurCents <= analysis.averageEurCents)
            assertTrue(analysis.averageEurCents <= analysis.maximumEurCents)
            assertTrue(analysis.observations.all { it.sourceUrl.startsWith("https://") })
        }
    }

    @Test
    fun purchasePriceSuggestionPrefersExactLineAverage() {
        val suggestion = BundledVerifiedCatalog.snapshot.suggestPurchasePrice(
            lineId = "mtn-montana-colors:mtn-94",
            volumeMl = 400,
        )

        assertEquals(528, suggestion?.priceEurCents)
        assertEquals(PurchasePriceSuggestionBasis.EXACT_LINE, suggestion?.basis)
        assertEquals(3, suggestion?.observationCount)
    }

    @Test
    fun everyCatalogLineGetsATraceableDefaultPrice() {
        canCatalog.flatMap { it.lines }.forEach { line ->
            val suggestion = BundledVerifiedCatalog.snapshot.suggestPurchasePrice(
                lineId = line.id,
                volumeMl = catalogDisplayVolumeMl(line.id),
            )

            assertTrue("Missing suggestion for ${line.id}", suggestion != null)
            assertTrue(suggestion!!.priceEurCents > 0)
            assertTrue(suggestion.observationCount > 0)
            assertTrue(suggestion.latestObservedAt?.isNotBlank() == true)
        }
    }
}
