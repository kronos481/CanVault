package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeWorkflowTest {
    @Test
    fun scannerCanAcceptFirstSuccessfullyDecodedBarcode() {
        val stabilizer = BarcodeStabilizer(requiredHits = 1)

        assertEquals(
            StableBarcode("4015962368101", "EAN-13"),
            stabilizer.offer("4015962368101", "EAN-13", 1_000),
        )
    }

    @Test
    fun scannerRequiresSameBarcodeTwice() {
        val stabilizer = BarcodeStabilizer(requiredHits = 2, maxGapMs = 900)

        assertNull(stabilizer.offer("4001234567890", "EAN-13", 1_000))
        assertEquals(
            StableBarcode("4001234567890", "EAN-13"),
            stabilizer.offer("4001234567890", "EAN-13", 1_350),
        )
    }

    @Test
    fun differentBarcodeRestartsConfirmation() {
        val stabilizer = BarcodeStabilizer(requiredHits = 2, maxGapMs = 900)

        assertNull(stabilizer.offer("11111111", "EAN-8", 1_000))
        assertNull(stabilizer.offer("22222222", "EAN-8", 1_200))
        assertEquals(
            StableBarcode("22222222", "EAN-8"),
            stabilizer.offer("22222222", "EAN-8", 1_400),
        )
    }

    @Test
    fun knownBarcodeRestoresSavedProductData() {
        val existing = CanItem(
            brandId = "molotow-belton",
            canLineId = "molotow-belton:burner-chrome-600-ml",
            colorName = "Chrome",
            externalBarcode = "0123456789012",
        )

        val result = resolveProductBarcode(
            snapshot = InventorySnapshot(cans = listOf(existing)),
            rawValue = "123456789012",
            format = "UPC-A",
        )

        assertTrue(result is ProductBarcodeResolution.Known)
        assertEquals(existing.id, (result as ProductBarcodeResolution.Known).can.id)
    }

    @Test
    fun unknownBarcodeStartsRealLearningFlow() {
        val result = resolveProductBarcode(
            snapshot = InventorySnapshot(),
            rawValue = "4001234567890",
            format = "EAN-13",
        )

        assertEquals(
            ProductBarcodeResolution.New("4001234567890", "EAN-13"),
            result,
        )
    }

    @Test
    fun verifiedCatalogWinsOnTheFirstScan() {
        val product = BundledVerifiedCatalog.snapshot.products.first { it.barcode == "4015962368101" }

        val result = resolveProductBarcode(
            snapshot = InventorySnapshot(),
            rawValue = "4015962368101",
            format = "EAN-13",
            verifiedCatalog = BundledVerifiedCatalog.snapshot,
        )

        assertTrue(result is ProductBarcodeResolution.Verified)
        assertEquals(product, (result as ProductBarcodeResolution.Verified).product)
    }

    @Test
    fun verifiedScanProducesCompleteAutomaticFormPrefill() {
        val product = BundledVerifiedCatalog.snapshot.products.first { it.barcode == "4015962368101" }
        val result = resolveProductBarcode(
            snapshot = InventorySnapshot(),
            rawValue = product.barcode,
            format = "EAN-13",
            verifiedCatalog = BundledVerifiedCatalog.snapshot,
        )

        val prefill = result.toProductBarcodePrefill()!!

        assertEquals(product.brandId, prefill.brandId)
        assertEquals(product.lineId, prefill.lineId)
        assertEquals(product.colorName, prefill.colorName)
        assertEquals(product.colorCode, prefill.colorCode)
        assertEquals(product.customHex, prefill.customHex)
        assertEquals(product.volumeMl, prefill.volumeMl)
        assertEquals(product.barcode, prefill.externalBarcode)
    }

    @Test
    fun learnedBarcodeRestoresEverySavedFormField() {
        val existing = CanItem(
            brandId = "molotow-belton",
            canLineId = "molotow-belton:burner-gold-600-ml",
            colorName = "Metallic Gold",
            colorCode = "940499",
            customHex = "#C29A45",
            volumeMl = 600,
            purchasePriceCents = 899,
            externalBarcode = "4015962368286",
        )
        val result = resolveProductBarcode(
            snapshot = InventorySnapshot(cans = listOf(existing)),
            rawValue = existing.externalBarcode!!,
            format = "EAN-13",
        )

        val prefill = result.toProductBarcodePrefill()!!

        assertEquals(existing.brandId, prefill.brandId)
        assertEquals(existing.canLineId, prefill.lineId)
        assertEquals(existing.colorName, prefill.colorName)
        assertEquals(existing.colorCode, prefill.colorCode)
        assertEquals(existing.customHex, prefill.customHex)
        assertEquals(existing.volumeMl, prefill.volumeMl)
        assertEquals(existing.purchasePriceCents, prefill.purchasePriceCents)
        assertTrue(prefill.sourceMessage.contains("Farbdaten"))
    }

    @Test
    fun upcAliasCanResolveVerifiedGtin() {
        val product = VerifiedCatalogProduct(
            barcode = "012345678905",
            brandId = "flame",
            lineId = "flame:flame-blue",
            colorName = "Test",
            sourceName = "Source",
            sourceUrl = "https://example.com/product",
            verifiedAt = "2026-08-02",
        )
        val catalog = VerifiedCatalogSnapshot(
            version = "test",
            publishedAt = "2026-08-02",
            products = listOf(product),
            prices = emptyList(),
        )

        val result = resolveProductBarcode(
            snapshot = InventorySnapshot(),
            rawValue = "0012345678905",
            format = "EAN-13",
            verifiedCatalog = catalog,
        )

        assertTrue(result is ProductBarcodeResolution.Verified)
    }
}
