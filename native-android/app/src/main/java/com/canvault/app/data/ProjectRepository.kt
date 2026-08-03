package com.canvault.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.projectsDataStore by preferencesDataStore(name = "canvault_projects_v1")

class ProjectRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storageKey = stringPreferencesKey("snapshot")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val snapshot = context.projectsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)
        .stateIn(scope, SharingStarted.Eagerly, ProjectSnapshot())

    suspend fun create(request: CreateProjectRequest): String {
        val project = CanProject(
            name = request.name.trim(),
            location = request.location.normalized(),
            notes = request.notes.normalized(),
            targetAreaM2 = request.targetAreaM2?.takeIf { it > 0.0 },
            coverageM2PerLiter = request.coverageM2PerLiter.coerceIn(0.5, 20.0),
            budgetCents = request.budgetCents?.coerceAtLeast(0),
            targetDate = request.targetDate.normalized(),
        )
        mutate { current -> current.copy(projects = listOf(project) + current.projects) }
        return project.id
    }

    suspend fun updateDetails(projectId: String, request: CreateProjectRequest) = updateProject(projectId) { project ->
        project.copy(
            name = request.name.trim(),
            location = request.location.normalized(),
            notes = request.notes.normalized(),
            targetAreaM2 = request.targetAreaM2?.takeIf { it > 0.0 },
            coverageM2PerLiter = request.coverageM2PerLiter.coerceIn(0.5, 20.0),
            budgetCents = request.budgetCents?.coerceAtLeast(0),
            targetDate = request.targetDate.normalized(),
        )
    }

    suspend fun setStatus(projectId: String, status: ProjectStatus) = updateProject(projectId) { project ->
        project.copy(status = status)
    }

    suspend fun setInventoryCans(projectId: String, canIds: Collection<String>) = updateProject(projectId) { project ->
        project.copy(inventoryCanIds = canIds.distinct())
    }

    suspend fun addBuyItem(projectId: String, request: AddProjectBuyItemRequest) = updateProject(projectId) { project ->
        val item = ProjectBuyItem(
            brandId = request.brandId,
            canLineId = request.canLineId,
            colorName = request.colorName.trim(),
            colorCode = request.colorCode.normalized(),
            customHex = request.customHex.normalized()?.uppercase(),
            volumeMl = request.volumeMl.coerceIn(50, 2_000),
            quantity = request.quantity.coerceIn(1, 99),
            unitPriceCents = request.unitPriceCents?.coerceAtLeast(0),
        )
        project.copy(buyItems = project.buyItems + item)
    }

    suspend fun toggleBuyItem(projectId: String, itemId: String) = updateProject(projectId) { project ->
        project.copy(
            buyItems = project.buyItems.map { item ->
                if (item.id == itemId) item.copy(purchased = !item.purchased) else item
            },
        )
    }

    suspend fun removeBuyItem(projectId: String, itemId: String) = updateProject(projectId) { project ->
        project.copy(buyItems = project.buyItems.filterNot { it.id == itemId })
    }

    suspend fun duplicate(projectId: String): String? {
        var duplicatedId: String? = null
        mutate { current ->
            val source = current.projects.firstOrNull { it.id == projectId } ?: return@mutate current
            val duplicate = source.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${source.name} – Kopie",
                status = ProjectStatus.PLANNING,
                inventoryCanIds = emptyList(),
                buyItems = source.buyItems.map { item ->
                    item.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        purchased = false,
                        createdAt = System.currentTimeMillis(),
                    )
                },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            duplicatedId = duplicate.id
            current.copy(projects = listOf(duplicate) + current.projects)
        }
        return duplicatedId
    }

    suspend fun delete(projectId: String) = mutate { current ->
        current.copy(projects = current.projects.filterNot { it.id == projectId })
    }

    private suspend fun updateProject(
        projectId: String,
        transform: (CanProject) -> CanProject,
    ) = mutate { current ->
        current.copy(
            projects = current.projects.map { project ->
                if (project.id == projectId) {
                    transform(project).copy(updatedAt = System.currentTimeMillis())
                } else {
                    project
                }
            },
        )
    }

    private suspend fun mutate(transform: (ProjectSnapshot) -> ProjectSnapshot) {
        context.projectsDataStore.edit { preferences ->
            val current = decode(preferences)
            preferences[storageKey] = json.encodeToString(transform(current))
        }
    }

    private fun decode(preferences: Preferences): ProjectSnapshot {
        val raw = preferences[storageKey] ?: return ProjectSnapshot()
        return try {
            json.decodeFromString<ProjectSnapshot>(raw)
        } catch (_: SerializationException) {
            ProjectSnapshot()
        }
    }
}

private fun String?.normalized(): String? = this?.trim()?.ifBlank { null }
