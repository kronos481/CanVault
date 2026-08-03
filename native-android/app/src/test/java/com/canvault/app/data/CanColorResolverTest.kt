package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanColorResolverTest {
    @Test
    fun explicitHexWins() {
        assertEquals(
            "#123ABC",
            resolveCanColorHex("#123abc", "940499", "Gold", "molotow-belton:burner-gold-600-ml"),
        )
    }

    @Test
    fun officialProductCodeResolvesToPublishedColor() {
        assertEquals(
            "#FFF9C3",
            resolveCanColorHex(null, "RV-189", null, "mtn-montana-colors:mtn-94"),
        )
    }

    @Test
    fun unresolvedLineDoesNotUseAnApproximation() {
        assertNull(resolveCanColorHex(null, "940397E", "Metallic Chrome", "molotow-belton:burner-chrome-600-ml"))
    }

    @Test
    fun commonColorWordsAreNeverGuessed() {
        assertNull(resolveCanColorHex(null, "UNKNOWN", "Signal Red", null))
        assertNull(resolveCanColorHex(null, "UNKNOWN", "Special Edition", null))
    }

    @Test
    fun exactManufacturerNameResolvesWithoutKeywordMatching() {
        assertEquals(
            "#FDF4A6",
            resolveCanColorHex(null, null, "jasmingelb", "molotow-belton:molotow-premium"),
        )
    }

    @Test
    fun officialManufacturerValueWinsOverAStaleStoredHex() {
        assertEquals(
            "#0C0C0A",
            resolveCanColorHex("#00FFFF", null, "Shock Black", "montana-cans:montana-gold"),
        )
    }

    @Test
    fun exactResolverDoesNotGuessFromAColorName() {
        val can = CanItem(
            brandId = "custom",
            canLineId = "custom",
            colorName = "Signal Red",
            colorCode = "UNKNOWN",
        )

        assertNull(resolveExactCanColorHex(can))
    }
}
