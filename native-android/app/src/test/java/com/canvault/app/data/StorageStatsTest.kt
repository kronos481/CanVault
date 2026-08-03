package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StorageStatsTest {
    private val zone = ZoneId.of("UTC")
    private val now = LocalDateTime.of(2026, 8, 3, 12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun historyStatsIncludeCurrentAndArchivedCansWithoutLosingSpend() {
        val current = can(
            id = "current",
            brand = "molotow-belton",
            line = "molotow-belton:premium",
            color = "Signal Red",
            hex = "#E53935",
            fill = 50,
            price = 700,
            volume = 400,
            status = CanStatus.OPENED,
            acquiredAt = now,
        )
        val archived = can(
            id = "archived",
            brand = "molotow-belton",
            line = "molotow-belton:premium",
            color = "Signal Red",
            hex = "#E53935",
            fill = 0,
            price = 900,
            volume = 600,
            status = CanStatus.ARCHIVED,
            acquiredAt = now - 10L * 86_400_000L,
        ).copy(statusBeforeArchive = CanStatus.EMPTY, archivedAt = now)

        val stats = calculateStorageStats(listOf(current, archived), nowMillis = now, zoneId = zone)

        assertEquals(2, stats.allTimeCanCount)
        assertEquals(1, stats.currentCanCount)
        assertEquals(1, stats.archivedCanCount)
        assertEquals(1, stats.emptyCanCount)
        assertEquals(1_600, stats.totalSpentCents)
        assertEquals(700, stats.currentInventoryValueCents)
        assertEquals(800, stats.averagePriceCents)
        assertEquals(1_000, stats.purchasedVolumeMl)
        assertEquals(200, stats.currentRemainingVolumeMl)
        assertEquals(800, stats.estimatedUsedVolumeMl)
        assertEquals(1, stats.distinctColorCount)
        assertEquals("molotow-belton", stats.topBrandId)
        assertEquals("Signal Red", stats.topUsedColorName)
        assertTrue(stats.monthlyActivity.sumOf(StorageMonthStat::addedCanCount) == 2)
    }

    @Test
    fun unopenedShareAndMonthlySpendUseRealCanDates() {
        val cans = listOf(
            can("new", "flame", "flame:blue", "Blue", "#1565C0", 100, 500, 400, CanStatus.IN_STOCK, now),
            can("open", "flame", "flame:blue", "Cyan", "#00B8D4", 80, 600, 400, CanStatus.OPENED, now),
        )

        val stats = calculateStorageStats(cans, nowMillis = now, zoneId = zone)

        assertEquals(50, stats.unopenedPercent)
        assertEquals(2, stats.monthlyActivity.last().addedCanCount)
        assertEquals(1_100, stats.monthlyActivity.last().spentCents)
    }

    private fun can(
        id: String,
        brand: String,
        line: String,
        color: String,
        hex: String,
        fill: Int,
        price: Int,
        volume: Int,
        status: CanStatus,
        acquiredAt: Long,
    ) = CanItem(
        id = id,
        brandId = brand,
        canLineId = line,
        colorName = color,
        customHex = hex,
        fillPercent = fill,
        purchasePriceCents = price,
        volumeMl = volume,
        status = status,
        acquiredAt = acquiredAt,
        createdAt = acquiredAt,
        updatedAt = now,
    )
}
