package com.canvault.app.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class InventoryColorSortKey(
    val group: Int,
    val hue: Double,
    val inverseLightness: Double,
    val inverseSaturation: Double,
    val label: String,
    val id: String,
)

internal fun inventoryColorSortKey(can: CanItem): InventoryColorSortKey {
    val hex = resolveExactCanColorHex(can)?.takeIf { it.matches(Regex("^#[0-9A-Fa-f]{6}$")) }
        ?: return InventoryColorSortKey(
            group = 2,
            hue = 0.0,
            inverseLightness = 0.0,
            inverseSaturation = 0.0,
            label = can.colorName.lowercase(),
            id = can.id,
        )
    val hsl = hexToHsl(hex)
    val neutral = hsl.chroma < 0.08 || hsl.saturation < 0.12

    return InventoryColorSortKey(
        // Chromatic colors follow the wheel first. White/grey/black form a
        // separate light-to-dark block; unresolved colors always stay last.
        group = if (neutral) 1 else 0,
        hue = if (neutral) 0.0 else (hsl.hue + 15.0) % 360.0,
        inverseLightness = -hsl.lightness,
        inverseSaturation = -hsl.saturation,
        label = can.colorName.lowercase(),
        id = can.id,
    )
}

internal val inventoryColorComparator: Comparator<CanItem> = Comparator { first, second ->
    val firstKey = inventoryColorSortKey(first)
    val secondKey = inventoryColorSortKey(second)
    compareValuesBy(
        firstKey,
        secondKey,
        InventoryColorSortKey::group,
        InventoryColorSortKey::hue,
        InventoryColorSortKey::inverseLightness,
        InventoryColorSortKey::inverseSaturation,
        InventoryColorSortKey::label,
        InventoryColorSortKey::id,
    )
}

private data class Hsl(
    val hue: Double,
    val saturation: Double,
    val lightness: Double,
    val chroma: Double,
)

private fun hexToHsl(hex: String): Hsl {
    val red = hex.substring(1, 3).toInt(16) / 255.0
    val green = hex.substring(3, 5).toInt(16) / 255.0
    val blue = hex.substring(5, 7).toInt(16) / 255.0
    val maximum = max(red, max(green, blue))
    val minimum = min(red, min(green, blue))
    val delta = maximum - minimum
    val lightness = (maximum + minimum) / 2.0
    val saturation = if (delta == 0.0) {
        0.0
    } else {
        delta / (1.0 - abs(2.0 * lightness - 1.0))
    }
    val hue = when {
        delta == 0.0 -> 0.0
        maximum == red -> 60.0 * (((green - blue) / delta) % 6.0)
        maximum == green -> 60.0 * ((blue - red) / delta + 2.0)
        else -> 60.0 * ((red - green) / delta + 4.0)
    }.let { if (it < 0.0) it + 360.0 else it }
    return Hsl(hue = hue, saturation = saturation, lightness = lightness, chroma = delta)
}
