package com.canvault.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CanVaultQrPayload(
    val app: String = "canvault",
    val version: Int = 1,
    val kind: String = "can",
    val brandId: String,
    val canLineId: String,
    val colorName: String,
    val colorCode: String? = null,
    val customHex: String? = null,
)

sealed interface ScanInterpretation {
    data class CatalogMatch(val payload: CanVaultQrPayload, val rawValue: String) : ScanInterpretation
    data class ExternalCode(val rawValue: String, val format: String) : ScanInterpretation
    data class Invalid(val message: String) : ScanInterpretation
}

object QrCodec {
    private const val MAX_LENGTH = 4096
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    fun encode(can: CanItem): String = json.encodeToString(
        CanVaultQrPayload(
            brandId = can.brandId,
            canLineId = can.canLineId,
            colorName = can.colorName,
            colorCode = can.colorCode,
            customHex = can.customHex,
        ),
    )

    fun interpret(raw: String, format: String): ScanInterpretation {
        val value = raw.trim()
        if (value.isEmpty()) return ScanInterpretation.Invalid("Der Code ist leer.")
        if (value.length > MAX_LENGTH) return ScanInterpretation.Invalid("Der Code ist zu lang.")

        val payload = try {
            json.decodeFromString<CanVaultQrPayload>(value)
        } catch (_: SerializationException) {
            return if (value.contains("\"app\"") && value.contains("canvault")) {
                ScanInterpretation.Invalid("Der CANVAULT-Code ist beschädigt.")
            } else {
                ScanInterpretation.ExternalCode(value, format)
            }
        } catch (_: IllegalArgumentException) {
            return ScanInterpretation.ExternalCode(value, format)
        }

        if (payload.app != "canvault" || payload.version != 1 || payload.kind != "can") {
            return ScanInterpretation.Invalid("Diese CANVAULT-Code-Version wird nicht unterstützt.")
        }
        val brand = catalogBrand(payload.brandId)
        val line = brand?.lines?.firstOrNull { it.id == payload.canLineId }
        if (brand == null || line == null || payload.colorName.isBlank()) {
            return ScanInterpretation.Invalid("Der Code passt nicht zum lokalen Katalog.")
        }
        val hex = payload.customHex
        if (hex != null && !Regex("^#[0-9A-Fa-f]{6}$").matches(hex)) {
            return ScanInterpretation.Invalid("Der Farbwert im Code ist ungültig.")
        }
        return ScanInterpretation.CatalogMatch(payload, value)
    }
}
