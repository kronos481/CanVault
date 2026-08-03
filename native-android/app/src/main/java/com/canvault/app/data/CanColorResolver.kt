package com.canvault.app.data

private val strictHexPattern = Regex("^#[0-9A-Fa-f]{6}$")

data class ResolvedCanColor(
    val hex: String,
    val officialColor: OfficialCanColor? = null,
    val source: OfficialCanColorSource? = null,
    val isUserProvided: Boolean = false,
)

fun resolveCanColor(can: CanItem): ResolvedCanColor? = resolveCanColor(
    customHex = can.customHex,
    colorCode = can.colorCode,
    colorName = can.colorName,
    lineId = can.canLineId,
)

fun resolveCanColor(
    customHex: String?,
    colorCode: String?,
    colorName: String?,
    lineId: String?,
): ResolvedCanColor? {
    OfficialCanColorCatalog.find(lineId, colorName, colorCode)?.let { official ->
        return ResolvedCanColor(
            hex = official.hex,
            officialColor = official,
            source = OfficialCanColorCatalog.sourceFor(official),
        )
    }

    normalizeHex(customHex)?.let { return ResolvedCanColor(it, isUserProvided = true) }
    normalizeHex(colorCode)?.let { return ResolvedCanColor(it, isUserProvided = true) }

    if (lineId != null) {
        catalogLine(lineId)?.defaultColorHex?.let(::normalizeHex)?.let { return ResolvedCanColor(it) }
    }

    return null
}

fun resolveCanColorHex(can: CanItem): String? = resolveCanColor(can)?.hex

fun resolveExactCanColorHex(can: CanItem): String? = resolveCanColor(can)?.hex

fun resolveCanColorHex(
    customHex: String?,
    colorCode: String?,
    colorName: String?,
    lineId: String?,
): String? = resolveCanColor(customHex, colorCode, colorName, lineId)?.hex

fun normalizeCanColorHex(value: String?): String? = normalizeHex(value)

private fun normalizeHex(value: String?): String? = value
    ?.trim()
    ?.uppercase()
    ?.takeIf(strictHexPattern::matches)
