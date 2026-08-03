package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorHarmonyTest {
    @Test
    fun ownedHexIsNeverChangedAndPaintAmountUsesFillAndQuantity() {
        val cans = listOf(
            testCan(id = "1", hex = "#123ABC", fill = 50, volume = 400),
            testCan(id = "2", hex = "#123abc", fill = 25, volume = 400),
        )

        val result = ColorHarmonyEngine.analyze(cans, includeMissingColors = false, toneCount = 2)

        assertEquals(1, result.inventoryColors.size)
        assertEquals("#123ABC", result.inventoryColors.single().hex)
        assertEquals(300, result.inventoryColors.single().effectiveMl)
        assertEquals(2, result.inventoryColors.single().canCount)
    }

    @Test
    fun inventoryModeNeverInventsAColorAndUsesRequestedToneCount() {
        val owned = listOf(
            testCan(id = "black", hex = "#111111", fill = 100, volume = 400),
            testCan(id = "yellow", hex = "#FFEA00", fill = 100, volume = 400),
            testCan(id = "white", hex = "#F7F7F2", fill = 100, volume = 400),
        )
        val ownedHex = owned.mapNotNull(::resolveExactCanColorHex).map(String::uppercase).toSet()
        val result = ColorHarmonyEngine.analyze(owned, includeMissingColors = false, toneCount = 2)

        assertTrue(result.palettes.isNotEmpty())
        assertTrue(result.palettes.all { it.swatches.size == 2 })
        assertTrue(result.palettes.flatMap(ColorHarmonyPalette::swatches).all(PaletteSwatch::isOwned))
        assertTrue(result.palettes.flatMap(ColorHarmonyPalette::swatches).all { it.hex in ownedHex })
    }

    @Test
    fun addColorModeUsesARealCatalogProductAsMissingSuggestion() {
        val result = ColorHarmonyEngine.analyze(
            listOf(testCan(id = "1", hex = "#E53935", fill = 100, volume = 400)),
            includeMissingColors = true,
            toneCount = 2,
        )
        val palette = result.palettes.first()
        val missing = palette.swatches.first { !it.isOwned }

        assertTrue(missing.hex.matches(Regex("^#[0-9A-F]{6}$")))
        assertTrue(missing.lineLabel != null)
        assertTrue(missing.sourceLabel != null)
        assertEquals(1, palette.missingCount)
        assertTrue(palette.minimumEdgeContrastRatio >= 3.0)
    }

    @Test
    fun darkOnDarkInventoryStillReturnsAnHonestlyRatedBestEffortPalette() {
        val result = ColorHarmonyEngine.analyze(
            listOf(
                testCan(id = "dark-1", hex = "#101216", fill = 100, volume = 400),
                testCan(id = "dark-2", hex = "#20242A", fill = 100, volume = 400),
                testCan(id = "dark-3", hex = "#303740", fill = 100, volume = 400),
            ),
            includeMissingColors = false,
            toneCount = 2,
        )

        assertTrue(result.palettes.isNotEmpty())
        assertTrue(result.palettes.all { palette -> palette.swatches.all(PaletteSwatch::isOwned) })
        assertTrue(result.palettes.all { it.scorePercent in 0..100 })
        assertTrue(result.palettes.any { it.isBestEffort && it.scorePercent < 55 })
    }

    @Test
    fun sevenToneInventoryPaletteUsesACompleteReadableLayout() {
        val result = ColorHarmonyEngine.analyze(
            listOf(
                testCan(id = "fill", hex = "#FF3B30", fill = 100, volume = 400),
                testCan(id = "outline", hex = "#111111", fill = 100, volume = 400),
                testCan(id = "second", hex = "#F5F5F5", fill = 100, volume = 400),
                testCan(id = "background", hex = "#16324F", fill = 100, volume = 400),
                testCan(id = "shadow", hex = "#8B1E1E", fill = 100, volume = 400),
                testCan(id = "fade", hex = "#FF9F0A", fill = 100, volume = 400),
                testCan(id = "highlight", hex = "#FFF4CC", fill = 100, volume = 400),
            ),
            includeMissingColors = false,
            toneCount = 7,
        )

        assertTrue(result.palettes.isNotEmpty())
        result.palettes.forEach { palette ->
            assertEquals(7, palette.swatches.size)
            assertTrue(palette.swatches.any { it.role == PaintRole.FILL })
            assertTrue(palette.swatches.any { it.role == PaintRole.OUTLINE })
            assertTrue(palette.minimumEdgeContrastRatio >= 3.0)
        }
        assertTrue(result.palettes.any { it.hasFillFade || it.fillColorCount > 1 })
    }

    @Test
    fun generatedKnowledgeBaseExceedsOneHundredThousandCombinations() {
        val result = ColorHarmonyEngine.analyze(
            listOf(testCan(id = "1", hex = "#E53935", fill = 100, volume = 400)),
            includeMissingColors = true,
            toneCount = 5,
        )

        assertTrue(result.knowledgeBaseCandidateCount >= 100_000)
        assertTrue(result.evaluatedCandidateCount > 0)
    }

    @Test
    fun fiveTonePalettesUseVariedReadableGraffitiLayouts() {
        val result = ColorHarmonyEngine.analyze(
            listOf(testCan(id = "anchor", hex = "#E53935", fill = 100, volume = 400)),
            includeMissingColors = true,
            toneCount = 5,
        )
        assertTrue(result.palettes.isNotEmpty())
        result.palettes.forEach { palette ->
            assertEquals(5, palette.swatches.size)
            assertTrue(palette.swatches.any { it.role == PaintRole.FILL })
            assertTrue(palette.swatches.any { it.role == PaintRole.OUTLINE })
            assertTrue(palette.minimumEdgeContrastRatio >= 3.2)
        }
        assertTrue(result.palettes.any { it.fillColorCount > 1 })
        assertTrue(result.palettes.any { it.swatches.any { swatch -> swatch.role == PaintRole.INLINE } })
    }

    @Test
    fun highlightsAreUsuallyLightAndMultiFillCanReplaceThem() {
        val result = ColorHarmonyEngine.analyze(
            listOf(testCan(id = "anchor", hex = "#E53935", fill = 100, volume = 400)),
            includeMissingColors = true,
            toneCount = 7,
        )
        val highlights = result.palettes.flatMap(ColorHarmonyPalette::swatches)
            .filter { it.role == PaintRole.INLINE }
        val lightHighlights = highlights.count { colorPerceptualLightness(it.hex) >= 0.78 }

        assertTrue(result.palettes.isNotEmpty())
        assertTrue(highlights.isNotEmpty())
        assertTrue(lightHighlights.toDouble() / highlights.size >= 0.70)
        assertTrue(result.palettes.any { it.fillColorCount >= 3 })
    }

    @Test
    fun addColorSuggestionsVaryProfilesAndActualCatalogColors() {
        val result = ColorHarmonyEngine.analyze(
            listOf(testCan(id = "anchor", hex = "#E53935", fill = 100, volume = 400)),
            includeMissingColors = true,
            toneCount = 5,
        )
        val profileNames = result.palettes.map { it.title.substringBefore(" · ") }.toSet()
        val missingSignatures = result.palettes.map { palette ->
            palette.swatches.filterNot(PaletteSwatch::isOwned).joinToString("|") { it.hex }
        }.toSet()

        assertTrue(result.palettes.size >= 6)
        assertTrue(profileNames.size >= 4)
        assertEquals(result.palettes.size, missingSignatures.size)
    }

    @Test
    fun inventorySuggestionsRotateTheFillAnchorInsteadOfRepeatingOneCombo() {
        val owned = listOf(
            testCan(id = "black", hex = "#111111", fill = 100, volume = 400),
            testCan(id = "white", hex = "#F7F7F2", fill = 100, volume = 400),
            testCan(id = "red", hex = "#E53935", fill = 100, volume = 400),
            testCan(id = "blue", hex = "#1565C0", fill = 100, volume = 400),
            testCan(id = "yellow", hex = "#FFD600", fill = 100, volume = 400),
            testCan(id = "cyan", hex = "#00B8D4", fill = 100, volume = 400),
        )
        val result = ColorHarmonyEngine.analyze(owned, includeMissingColors = false, toneCount = 3)
        val fills = result.palettes.map { palette ->
            palette.swatches.first { it.role == PaintRole.FILL }.hex
        }.toSet()

        assertTrue(result.palettes.size >= 4)
        assertTrue(fills.size >= 3)
    }

    @Test
    fun fiveToneInventorySearchFindsCombosAcrossTheOwnedColorSet() {
        val owned = listOf(
            testCan(id = "black", hex = "#111111", fill = 100, volume = 400),
            testCan(id = "white", hex = "#F7F7F2", fill = 100, volume = 400),
            testCan(id = "red", hex = "#E53935", fill = 100, volume = 400),
            testCan(id = "blue", hex = "#1565C0", fill = 100, volume = 400),
            testCan(id = "yellow", hex = "#FFD600", fill = 100, volume = 400),
            testCan(id = "cyan", hex = "#00B8D4", fill = 100, volume = 400),
            testCan(id = "orange", hex = "#FF6D00", fill = 100, volume = 400),
        )

        val result = ColorHarmonyEngine.analyze(owned, includeMissingColors = false, toneCount = 5)

        assertTrue(result.palettes.isNotEmpty())
        assertTrue(result.palettes.all { palette -> palette.swatches.all(PaletteSwatch::isOwned) })
        assertTrue(result.palettes.all { it.swatches.size == 5 })
    }

    @Test
    fun outlineAndBackgroundAreNeverPerceptuallySimilar() {
        val result = ColorHarmonyEngine.analyze(
            listOf(testCan(id = "anchor", hex = "#E53935", fill = 100, volume = 400)),
            includeMissingColors = true,
            toneCount = 5,
        )

        assertTrue(result.palettes.isNotEmpty())
        val palettesWithBackground = result.palettes.filter { palette ->
            palette.swatches.any { it.role == PaintRole.BACKGROUND }
        }
        assertTrue(palettesWithBackground.isNotEmpty())
        palettesWithBackground.forEach { palette ->
            val byRole = palette.swatches.associateBy(PaletteSwatch::role)
            val outline = byRole.getValue(PaintRole.OUTLINE)
            val background = byRole.getValue(PaintRole.BACKGROUND)
            assertTrue(colorPerceptualDistance(outline.hex, background.hex) >= 0.13)
            assertTrue(colorContrastRatio(outline.hex, background.hex) >= 1.8)
        }
    }

    @Test
    fun difficultInventoryColorsAreShownAsRatedPotentialInsteadOfBeingHidden() {
        val result = ColorHarmonyEngine.analyze(
            listOf(
                testCan(id = "green", hex = "#A8FF00", fill = 100, volume = 400),
                testCan(id = "purple", hex = "#6C00A2", fill = 100, volume = 400),
            ),
            includeMissingColors = false,
            toneCount = 2,
        )

        assertTrue(result.palettes.isNotEmpty())
        assertTrue(result.palettes.all { palette -> palette.swatches.all(PaletteSwatch::isOwned) })
        assertTrue(result.palettes.all { it.scorePercent in 0..100 })
    }

    @Test
    fun inventoryWithFewerColorsThanRequestedStillReturnsUsefulCombos() {
        val owned = listOf(
            testCan(id = "red", hex = "#E53935", fill = 100, volume = 400),
            testCan(id = "blue", hex = "#1565C0", fill = 100, volume = 400),
            testCan(id = "cream", hex = "#FFF4CC", fill = 100, volume = 400),
        )

        val result = ColorHarmonyEngine.analyze(owned, includeMissingColors = false, toneCount = 7)

        assertTrue(result.palettes.isNotEmpty())
        assertTrue(result.palettes.all { it.swatches.size == 3 })
        assertTrue(result.palettes.all { palette -> palette.swatches.all(PaletteSwatch::isOwned) })
        assertTrue(result.palettes.all { it.scorePercent in 0..100 })
    }

    @Test
    fun emptyArchivedAndUnknownColorsAreExcluded() {
        val activeUnknown = CanItem(
            id = "unknown",
            brandId = "custom",
            canLineId = "custom",
            colorName = "Mystery",
            colorCode = "NO-COLOR-DATA",
            fillPercent = 80,
        )
        val empty = testCan(id = "empty", hex = "#112233", fill = 100, volume = 400).copy(status = CanStatus.EMPTY)
        val archived = testCan(id = "archived", hex = "#445566", fill = 100, volume = 400).copy(status = CanStatus.ARCHIVED)

        val result = ColorHarmonyEngine.analyze(
            listOf(activeUnknown, empty, archived),
            includeMissingColors = true,
            toneCount = 5,
        )

        assertTrue(result.inventoryColors.isEmpty())
        assertTrue(result.palettes.isEmpty())
        assertEquals(1, result.unresolvedCanCount)
        assertFalse(result.totalEffectiveMl > 0)
    }

    private fun testCan(id: String, hex: String, fill: Int, volume: Int) = CanItem(
        id = id,
        brandId = "test",
        canLineId = "test-line",
        colorName = "Test color $id",
        customHex = hex,
        volumeMl = volume,
        fillPercent = fill,
    )
}
