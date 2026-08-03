package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialCanColorCatalogTest {
    @Test
    fun generatedCatalogHasValidHexAndUsefulCoverage() {
        assertTrue(OfficialCanColorCatalog.colors.size >= 1_800)
        assertTrue(OfficialCanColorCatalog.colors.all { it.hex.matches(Regex("^#[0-9A-F]{6}$")) })
        assertTrue(OfficialCanColorCatalog.sources.all { it.url.startsWith("https://") && it.extractedShadeCount > 0 })
        assertTrue(
            OfficialCanColorCatalog.colors.all { color ->
                OfficialCanColorCatalog.sources.any { it.id == color.sourceId }
            },
        )
        assertEquals(
            OfficialCanColorCatalog.colors.size,
            OfficialCanColorCatalog.colors
                .map { "${it.lineId}|${it.colorCode.orEmpty()}|${it.colorName}" }
                .distinct()
                .size,
        )
        assertTrue(OfficialCanColorCatalog.colorsForLine("mtn-montana-colors:mtn-94").size >= 200)
        assertEquals(215, OfficialCanColorCatalog.colorsForLine("montana-cans:montana-gold").size)
        assertTrue(OfficialCanColorCatalog.colorsForLine("molotow-belton:molotow-premium").size >= 200)
    }

    @Test
    fun exactNamesAndCodesResolveWithinTheirLine() {
        assertEquals(
            "#FFF9C3",
            OfficialCanColorCatalog.find("mtn-montana-colors:mtn-94", null, "RV-189")?.hex,
        )
        assertEquals(
            "#0C0C0A",
            OfficialCanColorCatalog.find("montana-cans:montana-gold", "Shock Black", null)?.hex,
        )
        assertEquals(
            "#1E71B8",
            OfficialCanColorCatalog.find("double-a:double-a", null, "DA-228")?.hex,
        )
    }

    @Test
    fun partialKeywordsNeverResolveAsExactColors() {
        assertNull(OfficialCanColorCatalog.find("molotow-belton:molotow-premium", "pink", null))
        assertNull(OfficialCanColorCatalog.find("mtn-montana-colors:mtn-94", "blue", null))
    }
}
