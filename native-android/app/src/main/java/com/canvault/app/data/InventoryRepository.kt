package com.canvault.app.data

import android.content.Context
import android.net.Uri
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
import java.io.File

private val Context.inventoryDataStore by preferencesDataStore(name = "canvault_inventory_v2")

class InventoryRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storageKey = stringPreferencesKey("snapshot")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val snapshot = context.inventoryDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)
        .stateIn(scope, SharingStarted.Eagerly, InventorySnapshot())

    suspend fun add(request: AddCanRequest): List<String> {
        val created = List(request.quantity.coerceIn(1, 99)) {
            CanItem(
                brandId = request.brandId,
                canLineId = request.canLineId,
                colorName = request.colorName.trim(),
                colorCode = request.colorCode?.trim()?.ifBlank { null },
                customHex = request.customHex?.trim()?.uppercase()?.ifBlank { null },
                volumeMl = request.volumeMl,
                fillPercent = request.fillPercent.coerceIn(0, 100),
                status = when (request.fillPercent.coerceIn(0, 100)) {
                    0 -> CanStatus.EMPTY
                    100 -> CanStatus.IN_STOCK
                    else -> CanStatus.OPENED
                },
                purchasePriceCents = request.purchasePriceCents,
                photoPath = request.photoPath,
                externalBarcode = request.externalBarcode,
            )
        }
        mutate { current ->
            val events = created.map { can ->
                CanEvent(canId = can.id, type = CanEventType.CREATED, description = "Dose hinzugefügt")
            }
            current.copy(cans = created + current.cans, events = events + current.events)
        }
        return created.map { it.id }
    }

    suspend fun updateFill(canId: String, percent: Int?) = mutate { current ->
        val old = current.cans.firstOrNull { it.id == canId } ?: return@mutate current
        val normalized = percent?.coerceIn(0, 100)
        val updated = old.copy(
            fillPercent = normalized,
            status = if (normalized == 0) CanStatus.EMPTY else old.status,
            updatedAt = System.currentTimeMillis(),
        )
        current.withUpdatedCan(
            updated,
            CanEvent(canId = canId, type = CanEventType.FILL_CHANGED, description = "Füllstand: ${normalized?.let { "$it %" } ?: "unbekannt"}"),
        )
    }

    suspend fun updateStatus(canId: String, status: CanStatus) = mutate { current ->
        val old = current.cans.firstOrNull { it.id == canId } ?: return@mutate current
        if (old.status == status) return@mutate current
        val updated = old.copy(status = status, updatedAt = System.currentTimeMillis())
        current.withUpdatedCan(
            updated,
            CanEvent(canId = canId, type = CanEventType.STATUS_CHANGED, description = "Status geändert"),
        )
    }

    suspend fun archive(canId: String) = mutate { current ->
        val old = current.cans.firstOrNull { it.id == canId } ?: return@mutate current
        if (old.status == CanStatus.ARCHIVED) return@mutate current
        val now = System.currentTimeMillis()
        val updated = old.copy(
            status = CanStatus.ARCHIVED,
            statusBeforeArchive = old.status,
            archivedAt = now,
            updatedAt = now,
        )
        current.withUpdatedCan(
            updated,
            CanEvent(canId = canId, type = CanEventType.ARCHIVED, description = "Dose archiviert"),
        )
    }

    suspend fun restore(canId: String) = mutate { current ->
        val old = current.cans.firstOrNull { it.id == canId } ?: return@mutate current
        if (old.status != CanStatus.ARCHIVED) return@mutate current
        val updated = old.copy(
            status = old.statusBeforeArchive ?: CanStatus.IN_STOCK,
            statusBeforeArchive = null,
            archivedAt = null,
            updatedAt = System.currentTimeMillis(),
        )
        current.withUpdatedCan(
            updated,
            CanEvent(canId = canId, type = CanEventType.RESTORED, description = "Dose wiederhergestellt"),
        )
    }

    suspend fun deleteArchivedPermanently(canId: String): Boolean {
        var deletedCan: CanItem? = null
        var deleteUnsharedPhoto = false
        mutate { current ->
            val candidate = current.cans.firstOrNull { it.id == canId } ?: return@mutate current
            if (candidate.status != CanStatus.ARCHIVED) return@mutate current

            val updated = current.withoutArchivedCan(canId)
            deletedCan = candidate
            deleteUnsharedPhoto = candidate.photoPath != null &&
                updated.cans.none { it.photoPath == candidate.photoPath }
            updated
        }

        if (deleteUnsharedPhoto) deletedCan?.photoPath?.let(::deleteOwnedCanPhoto)
        return deletedCan != null
    }

    private suspend fun mutate(transform: (InventorySnapshot) -> InventorySnapshot) {
        context.inventoryDataStore.edit { preferences ->
            val current = decode(preferences)
            preferences[storageKey] = json.encodeToString(transform(current))
        }
    }

    private fun decode(preferences: Preferences): InventorySnapshot {
        val raw = preferences[storageKey] ?: return InventorySnapshot()
        return try {
            json.decodeFromString<InventorySnapshot>(raw)
        } catch (_: SerializationException) {
            InventorySnapshot()
        }
    }

    private fun deleteOwnedCanPhoto(photoPath: String) {
        runCatching {
            val uri = Uri.parse(photoPath)
            if (uri.scheme != "file") return
            val target = File(requireNotNull(uri.path)).canonicalFile
            val photoDirectory = File(context.filesDir, "can_photos").canonicalFile
            if (target.parentFile == photoDirectory) target.delete()
        }
    }
}

private fun InventorySnapshot.withUpdatedCan(can: CanItem, event: CanEvent) = copy(
    cans = cans.map { if (it.id == can.id) can else it },
    events = listOf(event) + events,
)

internal fun InventorySnapshot.withoutArchivedCan(canId: String): InventorySnapshot {
    val candidate = cans.firstOrNull { it.id == canId } ?: return this
    if (candidate.status != CanStatus.ARCHIVED) return this
    return copy(
        cans = cans.filterNot { it.id == canId },
        events = events.filterNot { it.canId == canId },
    )
}
