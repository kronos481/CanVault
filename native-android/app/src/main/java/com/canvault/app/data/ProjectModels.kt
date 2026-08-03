package com.canvault.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ProjectStatus {
    PLANNING,
    READY,
    ACTIVE,
    COMPLETED,
    ARCHIVED,
}

@Serializable
data class ProjectBuyItem(
    val id: String = UUID.randomUUID().toString(),
    val brandId: String,
    val canLineId: String,
    val colorName: String,
    val colorCode: String? = null,
    val customHex: String? = null,
    val volumeMl: Int = 400,
    val quantity: Int = 1,
    val unitPriceCents: Int? = null,
    val purchased: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class CanProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String? = null,
    val notes: String? = null,
    val targetAreaM2: Double? = null,
    val coverageM2PerLiter: Double = 5.0,
    val budgetCents: Int? = null,
    val targetDate: String? = null,
    val status: ProjectStatus = ProjectStatus.PLANNING,
    val inventoryCanIds: List<String> = emptyList(),
    val buyItems: List<ProjectBuyItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ProjectSnapshot(
    val projects: List<CanProject> = emptyList(),
)

data class CreateProjectRequest(
    val name: String,
    val location: String?,
    val notes: String?,
    val targetAreaM2: Double?,
    val coverageM2PerLiter: Double,
    val budgetCents: Int?,
    val targetDate: String?,
)

data class AddProjectBuyItemRequest(
    val brandId: String,
    val canLineId: String,
    val colorName: String,
    val colorCode: String?,
    val customHex: String?,
    val volumeMl: Int,
    val quantity: Int,
    val unitPriceCents: Int?,
)
