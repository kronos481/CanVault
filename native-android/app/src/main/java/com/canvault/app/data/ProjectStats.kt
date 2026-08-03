package com.canvault.app.data

import kotlin.math.ceil
import kotlin.math.roundToInt

data class ProjectStats(
    val selectedCanCount: Int,
    val availableMl: Int,
    val readyMl: Int,
    val plannedMl: Int,
    val inventoryValueCents: Long,
    val buyListCostCents: Long,
    val outstandingCostCents: Long,
    val projectedCostCents: Long,
    val readyCoverageM2: Double,
    val plannedCoverageM2: Double,
    val coveragePercent: Int?,
    val readyPercent: Int?,
    val shortageMl: Int,
    val readyShortageMl: Int,
    val budgetRemainingCents: Long?,
    val openBuyUnits: Int,
    val purchasedBuyUnits: Int,
)

fun CanItem.remainingVolumeMl(): Int {
    if (status == CanStatus.ARCHIVED) return 0
    return ((volumeMl ?: 0) * (fillPercent ?: 0) / 100.0).roundToInt().coerceAtLeast(0)
}

fun calculateProjectStats(
    project: CanProject,
    inventory: List<CanItem>,
): ProjectStats {
    val selected = project.inventoryCanIds.mapNotNull { id -> inventory.firstOrNull { it.id == id } }
    val availableMl = selected.sumOf(CanItem::remainingVolumeMl)
    val inventoryValueCents = selected.sumOf { can ->
        val price = can.purchasePriceCents ?: 0
        (price * (can.fillPercent ?: 0) / 100.0).roundToInt().toLong()
    }
    val buyListMl = project.buyItems.sumOf { it.volumeMl * it.quantity }
    val purchasedMl = project.buyItems.filter(ProjectBuyItem::purchased).sumOf { it.volumeMl * it.quantity }
    val buyListCost = project.buyItems.sumOf { (it.unitPriceCents ?: 0).toLong() * it.quantity }
    val outstandingCost = project.buyItems
        .filterNot(ProjectBuyItem::purchased)
        .sumOf { (it.unitPriceCents ?: 0).toLong() * it.quantity }
    val readyMl = availableMl + purchasedMl
    val plannedMl = availableMl + buyListMl
    val readyCoverage = readyMl / 1_000.0 * project.coverageM2PerLiter
    val plannedCoverage = plannedMl / 1_000.0 * project.coverageM2PerLiter
    val target = project.targetAreaM2
    val requiredMl = target?.let { ceil(it / project.coverageM2PerLiter * 1_000.0).toInt() } ?: 0
    val projectedCost = inventoryValueCents + buyListCost

    return ProjectStats(
        selectedCanCount = selected.size,
        availableMl = availableMl,
        readyMl = readyMl,
        plannedMl = plannedMl,
        inventoryValueCents = inventoryValueCents,
        buyListCostCents = buyListCost,
        outstandingCostCents = outstandingCost,
        projectedCostCents = projectedCost,
        readyCoverageM2 = readyCoverage,
        plannedCoverageM2 = plannedCoverage,
        coveragePercent = target?.takeIf { it > 0.0 }?.let { (plannedCoverage / it * 100.0).roundToInt().coerceIn(0, 999) },
        readyPercent = target?.takeIf { it > 0.0 }?.let { (readyCoverage / it * 100.0).roundToInt().coerceIn(0, 999) },
        shortageMl = (requiredMl - plannedMl).coerceAtLeast(0),
        readyShortageMl = (requiredMl - readyMl).coerceAtLeast(0),
        budgetRemainingCents = project.budgetCents?.toLong()?.minus(projectedCost),
        openBuyUnits = project.buyItems.filterNot(ProjectBuyItem::purchased).sumOf(ProjectBuyItem::quantity),
        purchasedBuyUnits = project.buyItems.filter(ProjectBuyItem::purchased).sumOf(ProjectBuyItem::quantity),
    )
}
