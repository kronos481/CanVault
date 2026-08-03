package com.canvault.app.data

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

data class StorageMonthStat(
    val yearMonth: YearMonth,
    val addedCanCount: Int,
    val spentCents: Int,
)

data class StorageStats(
    val allTimeCanCount: Int,
    val currentCanCount: Int,
    val archivedCanCount: Int,
    val emptyCanCount: Int,
    val totalSpentCents: Int,
    val currentInventoryValueCents: Int,
    val averagePriceCents: Int?,
    val mostExpensiveCanCents: Int?,
    val purchasedVolumeMl: Int,
    val currentRemainingVolumeMl: Int,
    val estimatedUsedVolumeMl: Int,
    val distinctColorCount: Int,
    val unopenedPercent: Int,
    val averageUsageDays: Int?,
    val topBrandId: String?,
    val topLineId: String?,
    val topUsedColorName: String?,
    val monthlyActivity: List<StorageMonthStat>,
)

fun calculateStorageStats(
    cans: List<CanItem>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): StorageStats {
    val current = cans.filter { it.status != CanStatus.ARCHIVED }
    val priced = cans.mapNotNull(CanItem::purchasePriceCents)
    val remainingByCan = cans.associateWith(::estimatedRemainingMl)
    val usedByCan = cans.associateWith { can ->
        (can.volumeMl ?: 400).coerceAtLeast(0) - remainingByCan.getValue(can)
    }
    val completed = cans.filter { can ->
        can.status == CanStatus.ARCHIVED || can.status == CanStatus.EMPTY || can.fillPercent == 0
    }
    val usageDays = completed.mapNotNull { can ->
        val end = can.archivedAt ?: can.updatedAt
        val start = can.acquiredAt.takeIf { it > 0 } ?: can.createdAt
        (end - start).takeIf { it >= 0 }?.let { duration -> duration / 86_400_000.0 }
    }
    val nowMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
    val months = (5 downTo 0).map { nowMonth.minusMonths(it.toLong()) }
    val monthlyActivity = months.map { month ->
        val inMonth = cans.filter { can ->
            YearMonth.from(Instant.ofEpochMilli(can.acquiredAt).atZone(zoneId)) == month
        }
        StorageMonthStat(
            yearMonth = month,
            addedCanCount = inMonth.size,
            spentCents = inMonth.sumOf { it.purchasePriceCents ?: 0 },
        )
    }
    val usedByColor = cans
        .groupBy { it.colorName.trim().ifBlank { "Unbenannte Farbe" } }
        .mapValues { (_, entries) -> entries.sumOf { usedByCan.getValue(it) } }
    val topUsedColor = usedByColor.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .firstOrNull { it.value > 0 }
        ?.key
    val distinctColors = cans.map { can ->
        resolveExactCanColorHex(can)?.uppercase() ?: can.colorName.trim().lowercase()
    }.filter(String::isNotBlank).toSet().size

    return StorageStats(
        allTimeCanCount = cans.size,
        currentCanCount = current.size,
        archivedCanCount = cans.count { it.status == CanStatus.ARCHIVED },
        emptyCanCount = cans.count { can ->
            can.fillPercent == 0 || can.status == CanStatus.EMPTY || can.statusBeforeArchive == CanStatus.EMPTY
        },
        totalSpentCents = priced.sum(),
        currentInventoryValueCents = current.sumOf { it.purchasePriceCents ?: 0 },
        averagePriceCents = priced.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
        mostExpensiveCanCents = priced.maxOrNull(),
        purchasedVolumeMl = cans.sumOf { (it.volumeMl ?: 400).coerceAtLeast(0) },
        currentRemainingVolumeMl = current.sumOf { remainingByCan.getValue(it) },
        estimatedUsedVolumeMl = usedByCan.values.sum(),
        distinctColorCount = distinctColors,
        unopenedPercent = if (current.isEmpty()) 0 else {
            (current.count { it.status == CanStatus.IN_STOCK && (it.fillPercent ?: 100) == 100 } * 100.0 / current.size)
                .roundToInt()
        },
        averageUsageDays = usageDays.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
        topBrandId = mostFrequentId(cans.map(CanItem::brandId)),
        topLineId = mostFrequentId(cans.map(CanItem::canLineId)),
        topUsedColorName = topUsedColor,
        monthlyActivity = monthlyActivity,
    )
}

private fun estimatedRemainingMl(can: CanItem): Int {
    val volume = (can.volumeMl ?: 400).coerceAtLeast(0)
    val fill = (can.fillPercent ?: 100).coerceIn(0, 100)
    return (volume * fill / 100.0).roundToInt()
}

private fun mostFrequentId(values: List<String>): String? = values
    .filter(String::isNotBlank)
    .groupingBy { it }
    .eachCount()
    .entries
    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
    .firstOrNull()
    ?.key
