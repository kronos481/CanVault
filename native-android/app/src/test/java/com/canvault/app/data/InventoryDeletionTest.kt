package com.canvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class InventoryDeletionTest {
    @Test
    fun permanentDeleteRemovesOnlyTheSelectedArchivedCanAndItsEvents() {
        val archived = can("archived", CanStatus.ARCHIVED)
        val otherArchived = can("other-archived", CanStatus.ARCHIVED)
        val active = can("active", CanStatus.IN_STOCK)
        val snapshot = InventorySnapshot(
            cans = listOf(archived, otherArchived, active),
            events = listOf(
                event("archived"),
                event("archived"),
                event("other-archived"),
                event("active"),
            ),
        )

        val result = snapshot.withoutArchivedCan("archived")

        assertEquals(listOf("other-archived", "active"), result.cans.map(CanItem::id))
        assertEquals(listOf("other-archived", "active"), result.events.map(CanEvent::canId))
    }

    @Test
    fun permanentDeleteRefusesToRemoveAnActiveCan() {
        val snapshot = InventorySnapshot(
            cans = listOf(can("active", CanStatus.IN_STOCK)),
            events = listOf(event("active")),
        )

        val result = snapshot.withoutArchivedCan("active")

        assertSame(snapshot, result)
    }

    @Test
    fun permanentDeleteIgnoresAnUnknownId() {
        val snapshot = InventorySnapshot(cans = listOf(can("archived", CanStatus.ARCHIVED)))

        val result = snapshot.withoutArchivedCan("missing")

        assertSame(snapshot, result)
    }

    private fun can(id: String, status: CanStatus) = CanItem(
        id = id,
        brandId = "test-brand",
        canLineId = "test-line",
        colorName = "Test $id",
        status = status,
    )

    private fun event(canId: String) = CanEvent(
        canId = canId,
        type = CanEventType.CREATED,
        description = "Test",
    )
}
