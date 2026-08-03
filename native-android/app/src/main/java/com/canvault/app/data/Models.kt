package com.canvault.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class CanStatus {
    IN_STOCK,
    OPENED,
    RESERVED,
    EMPTY,
    COLLECTION,
    ARCHIVED,
}

@Serializable
data class CanItem(
    val id: String = UUID.randomUUID().toString(),
    val brandId: String,
    val canLineId: String,
    val colorName: String,
    val colorCode: String? = null,
    val customHex: String? = null,
    val volumeMl: Int? = 400,
    val fillPercent: Int? = 100,
    val status: CanStatus = CanStatus.IN_STOCK,
    val statusBeforeArchive: CanStatus? = null,
    val purchasePriceCents: Int? = null,
    val currency: String = "EUR",
    val photoPath: String? = null,
    val externalBarcode: String? = null,
    val acquiredAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
enum class CanEventType { CREATED, FILL_CHANGED, STATUS_CHANGED, ARCHIVED, RESTORED }

@Serializable
data class CanEvent(
    val id: String = UUID.randomUUID().toString(),
    val canId: String,
    val type: CanEventType,
    val description: String,
    val occurredAt: Long = System.currentTimeMillis(),
)

@Serializable
data class InventorySnapshot(
    val cans: List<CanItem> = emptyList(),
    val events: List<CanEvent> = emptyList(),
)

data class AddCanRequest(
    val brandId: String,
    val canLineId: String,
    val colorName: String,
    val colorCode: String?,
    val customHex: String?,
    val volumeMl: Int?,
    val fillPercent: Int,
    val quantity: Int,
    val purchasePriceCents: Int?,
    val photoPath: String?,
    val externalBarcode: String?,
)
