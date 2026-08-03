package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryColorSortTest {
    @Test
    fun chromaticColorsFollowTheColorWheelThenNeutralsAndUnknowns() {
        val cans = listOf(
            canWithColor("unknown", null),
            canWithColor("black", "#050505"),
            canWithColor("purple", "#8E24AA"),
            canWithColor("blue", "#1565C0"),
            canWithColor("green", "#2EAD55"),
            canWithColor("yellow", "#FFD600"),
            canWithColor("orange", "#FF7A00"),
            canWithColor("red", "#E53935"),
            canWithColor("white", "#F7F7F2"),
            canWithColor("cyan", "#00B8D4"),
        )

        val sortedIds = cans.sortedWith(inventoryColorComparator).map(CanItem::id)

        assertEquals(
            listOf("red", "orange", "yellow", "green", "cyan", "blue", "purple", "white", "black", "unknown"),
            sortedIds,
        )
    }

    @Test
    fun shadesOfTheSameHueAreSortedLightToDark() {
        val cans = listOf(
            canWithColor("dark-red", "#780000"),
            canWithColor("light-red", "#FF8585"),
            canWithColor("mid-red", "#D92323"),
        )

        assertEquals(
            listOf("light-red", "mid-red", "dark-red"),
            cans.sortedWith(inventoryColorComparator).map(CanItem::id),
        )
    }

    private fun canWithColor(id: String, hex: String?) = CanItem(
        id = id,
        brandId = "test",
        canLineId = "test-line",
        colorName = id,
        customHex = hex,
    )
}
