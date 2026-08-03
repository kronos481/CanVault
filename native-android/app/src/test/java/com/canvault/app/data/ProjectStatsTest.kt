package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectStatsTest {
    @Test
    fun calculatesCostVolumeCoverageAndReadiness() {
        val inventoryCan = CanItem(
            id = "inventory-1",
            brandId = "montana-cans",
            canLineId = "montana-cans:montana-black",
            colorName = "Black",
            volumeMl = 400,
            fillPercent = 50,
            purchasePriceCents = 500,
        )
        val project = CanProject(
            name = "Wall",
            targetAreaM2 = 6.0,
            coverageM2PerLiter = 5.0,
            budgetCents = 2_500,
            inventoryCanIds = listOf(inventoryCan.id),
            buyItems = listOf(
                ProjectBuyItem(
                    brandId = "montana-cans",
                    canLineId = "montana-cans:montana-black",
                    colorName = "White",
                    volumeMl = 400,
                    quantity = 2,
                    unitPriceCents = 600,
                ),
                ProjectBuyItem(
                    brandId = "molotow-belton",
                    canLineId = "molotow-belton:molotow-burner",
                    colorName = "Chrome",
                    volumeMl = 600,
                    quantity = 1,
                    unitPriceCents = 800,
                    purchased = true,
                ),
            ),
        )

        val stats = calculateProjectStats(project, listOf(inventoryCan))

        assertEquals(1, stats.selectedCanCount)
        assertEquals(200, stats.availableMl)
        assertEquals(800, stats.readyMl)
        assertEquals(1_600, stats.plannedMl)
        assertEquals(250L, stats.inventoryValueCents)
        assertEquals(2_000L, stats.buyListCostCents)
        assertEquals(1_200L, stats.outstandingCostCents)
        assertEquals(2_250L, stats.projectedCostCents)
        assertEquals(4.0, stats.readyCoverageM2, 0.001)
        assertEquals(8.0, stats.plannedCoverageM2, 0.001)
        assertEquals(133, stats.coveragePercent)
        assertEquals(67, stats.readyPercent)
        assertEquals(0, stats.shortageMl)
        assertEquals(400, stats.readyShortageMl)
        assertEquals(250L, stats.budgetRemainingCents)
        assertEquals(2, stats.openBuyUnits)
        assertEquals(1, stats.purchasedBuyUnits)
    }

    @Test
    fun archivedInventoryDoesNotCountAndTargetCanBeOptional() {
        val archived = CanItem(
            id = "archived",
            brandId = "loop-colors",
            canLineId = "loop-colors:loop-400-ml",
            colorName = "Black",
            volumeMl = 400,
            fillPercent = 100,
            status = CanStatus.ARCHIVED,
        )
        val project = CanProject(name = "Sketch", inventoryCanIds = listOf(archived.id))

        val stats = calculateProjectStats(project, listOf(archived))

        assertEquals(0, stats.availableMl)
        assertEquals(0.0, stats.plannedCoverageM2, 0.001)
        assertNull(stats.coveragePercent)
        assertNull(stats.readyPercent)
    }
}
