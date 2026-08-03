package com.canvault.app.data

data class CatalogBrand(
    val id: String,
    val displayName: String,
    val lines: List<CatalogLine>,
)

data class CatalogLine(
    val id: String,
    val displayName: String,
    val defaultVolumeMl: Int? = null,
    val defaultColorName: String? = null,
    val defaultColorCode: String? = null,
    val defaultColorHex: String? = null,
)

private fun slug(value: String): String = value
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')

private fun brand(id: String, name: String, vararg lines: String) = CatalogBrand(
    id = id,
    displayName = name,
    lines = lines.map { CatalogLine("$id:${slug(it)}", it) },
)

private val molotowCatalog = CatalogBrand(
    id = "molotow-belton",
    displayName = "Molotow / Belton",
    lines = listOf(
        CatalogLine("molotow-belton:molotow-premium", "Molotow Premium"),
        CatalogLine("molotow-belton:molotow-burner", "Molotow Burner"),
        CatalogLine(
            id = "molotow-belton:burner-chrome-600-ml",
            displayName = "Burner Chrome 600 ml",
            defaultVolumeMl = 600,
            defaultColorName = "Metallic Chrome",
            defaultColorCode = "940397E",
        ),
        CatalogLine(
            id = "molotow-belton:burner-gold-600-ml",
            displayName = "Burner Gold 600 ml",
            defaultVolumeMl = 600,
            defaultColorName = "Metallic Gold",
            defaultColorCode = "940499",
        ),
        CatalogLine(
            id = "molotow-belton:burner-copper-600-ml",
            displayName = "Burner Copper 600 ml",
            defaultVolumeMl = 600,
            defaultColorName = "Metallic Copper",
            defaultColorCode = "940500",
        ),
        CatalogLine(
            id = "molotow-belton:burner-black-600-ml",
            displayName = "Burner Black 600 ml",
            defaultVolumeMl = 600,
            defaultColorName = "Black",
            defaultColorCode = "940398",
        ),
        CatalogLine("molotow-belton:molotow-coversall", "Molotow CoversAll"),
    ),
)

val canCatalog = listOf(
    brand("mtn-montana-colors", "MTN / Montana Colors", "MTN 94", "MTN Hardcore", "MTN Vice", "MTN Water Based 400", "MTN Mega", "MTN Alien"),
    brand("montana-cans", "Montana Cans", "Montana Black", "Montana Gold", "Montana White", "Montana Tarblack", "Montana Blackout Tarblack", "Montana Ultra Wide"),
    molotowCatalog,
    brand("loop-colors", "Loop Colors", "Loop 400 ml", "Loop Asphalt"),
    brand("flame", "Flame", "Flame Blue", "Flame Orange"),
    brand("kobra", "Kobra", "Kobra HP", "Kobra LP"),
    brand("ironlak", "Ironlak", "Ironlak 400 ml", "Sugar Artists Acrylic"),
    brand("nbq", "NBQ", "NBQ Fast", "NBQ Slow"),
    brand("dope", "Dope", "Dope Action", "Dope Classic"),
    brand("dang", "Dang", "Dang Prime", "Dang Hi-Flow"),
    brand("clash", "Clash", "Clash"),
    brand("beat", "Beat", "Beat"),
    brand("scribo", "Scribo", "Scribo"),
    brand("double-a", "Double A", "Double A"),
    brand("krink", "Krink", "Krink K-750"),
)

fun catalogBrand(id: String): CatalogBrand? = canCatalog.firstOrNull { it.id == id }

fun catalogLine(id: String): CatalogLine? = canCatalog
    .asSequence()
    .flatMap { it.lines.asSequence() }
    .firstOrNull { it.id == id }

fun brandName(id: String): String = catalogBrand(id)?.displayName ?: id

fun lineName(id: String): String = catalogLine(id)?.displayName ?: id.substringAfter(':', id)
