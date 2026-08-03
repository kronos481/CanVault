package com.canvault.app.data

data class StableBarcode(
    val rawValue: String,
    val format: String,
)

class BarcodeStabilizer(
    private val requiredHits: Int = 2,
    private val maxGapMs: Long = 900L,
) {
    init {
        require(requiredHits >= 1)
        require(maxGapMs > 0)
    }

    private var candidate: StableBarcode? = null
    private var hits: Int = 0
    private var lastSeenAt: Long = 0L

    @Synchronized
    fun offer(rawValue: String, format: String, nowMs: Long): StableBarcode? {
        val value = rawValue.trim()
        if (value.isBlank()) return null

        val current = StableBarcode(value, format)
        val sameCandidate = candidate == current && nowMs - lastSeenAt <= maxGapMs
        if (sameCandidate) {
            hits += 1
        } else {
            candidate = current
            hits = 1
        }
        lastSeenAt = nowMs

        return current.takeIf { hits >= requiredHits }
    }
}

sealed interface ProductBarcodeResolution {
    data class Verified(
        val product: VerifiedCatalogProduct,
        val scannedValue: String,
        val format: String,
    ) : ProductBarcodeResolution
    data class Known(val can: CanItem, val scannedValue: String, val format: String) : ProductBarcodeResolution
    data class New(val scannedValue: String, val format: String) : ProductBarcodeResolution
    data class Invalid(val message: String) : ProductBarcodeResolution
}

data class ProductBarcodePrefill(
    val brandId: String,
    val lineId: String,
    val colorName: String,
    val colorCode: String?,
    val customHex: String?,
    val volumeMl: Int?,
    val purchasePriceCents: Int?,
    val externalBarcode: String,
    val sourceMessage: String,
)

fun ProductBarcodeResolution.toProductBarcodePrefill(): ProductBarcodePrefill? = when (this) {
    is ProductBarcodeResolution.Verified -> ProductBarcodePrefill(
        brandId = product.brandId,
        lineId = product.lineId,
        colorName = product.colorName,
        colorCode = product.colorCode,
        customHex = product.customHex,
        volumeMl = product.volumeMl,
        purchasePriceCents = null,
        externalBarcode = scannedValue,
        sourceMessage = "Im verifizierten Katalog gefunden · Quelle: ${product.sourceName} · geprüft ${product.verifiedAt}.",
    )
    is ProductBarcodeResolution.Known -> ProductBarcodePrefill(
        brandId = can.brandId,
        lineId = can.canLineId,
        colorName = can.colorName,
        colorCode = can.colorCode,
        customHex = can.customHex,
        volumeMl = can.volumeMl,
        purchasePriceCents = can.purchasePriceCents,
        externalBarcode = scannedValue,
        sourceMessage = "Barcode bekannt – gespeicherte Produkt- und Farbdaten wurden automatisch übernommen.",
    )
    is ProductBarcodeResolution.New,
    is ProductBarcodeResolution.Invalid,
    -> null
}

fun resolveProductBarcode(
    snapshot: InventorySnapshot,
    rawValue: String,
    format: String,
    verifiedCatalog: VerifiedCatalogSnapshot? = null,
): ProductBarcodeResolution {
    val value = rawValue.trim()
    if (value.isBlank()) return ProductBarcodeResolution.Invalid("Der Barcode ist leer.")
    if (value.length > 512) return ProductBarcodeResolution.Invalid("Der Barcode ist zu lang.")

    val verifiedProduct = verifiedCatalog?.findVerifiedProduct(value)
    if (verifiedProduct != null) {
        return ProductBarcodeResolution.Verified(verifiedProduct, value, format)
    }

    val knownCan = snapshot.cans
        .asSequence()
        .filter { can ->
            val stored = can.externalBarcode
            stored != null && barcodesEquivalent(stored, value)
        }
        .sortedWith(
            compareByDescending<CanItem> { it.status != CanStatus.ARCHIVED }
                .thenByDescending { it.updatedAt },
        )
        .firstOrNull()

    return if (knownCan != null) {
        ProductBarcodeResolution.Known(knownCan, value, format)
    } else {
        ProductBarcodeResolution.New(value, format)
    }
}

internal fun barcodesEquivalent(first: String, second: String): Boolean =
    barcodeAliases(first).intersect(barcodeAliases(second)).isNotEmpty()

private fun barcodeAliases(rawValue: String): Set<String> {
    val value = rawValue.trim()
    if (!value.all(Char::isDigit)) return setOf(value)

    return buildSet {
        add(value)
        if (value.length == 12) add("0$value")
        if (value.length == 13 && value.startsWith('0')) add(value.drop(1))
    }
}
