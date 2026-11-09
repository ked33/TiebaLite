package com.huanchengfly.tieba.post.arch

import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ImmutableListsTest {
    private data class Item(val id: Long, val title: String)

    @Test
    fun refreshKeepsNewVersionsFirstAndRetainsAllOldItems() {
        val refreshed = Item(2, "new")
        val old = listOf(Item(1, "one"), Item(2, "old"), Item(3, "three"))
        val incoming = listOf(refreshed, Item(4, "four"), Item(2, "duplicate"))

        val result = incoming.mergeDistinct(old) { it.id }

        assertEquals(listOf(2L, 4L, 1L, 3L), result.map { it.id })
        assertSame(refreshed, result.first())
        assertEquals((incoming + old).distinctBy { it.id }, result)
    }

    @Test
    fun loadMoreKeepsExistingVersionsAndFirstIncomingOccurrence() {
        val existing = Item(1, "existing")
        val old = listOf(existing, Item(1, "old duplicate"))
        val incoming = listOf(Item(1, "new"), Item(2, "first"), Item(2, "second"))

        val result = old.mergeDistinct(incoming) { it.id }

        assertEquals(listOf(existing, incoming[1]), result)
        assertSame(existing, result.first())
    }

    @Test
    fun mergeHandlesEmptyListsWithoutChangingOrder() {
        val items = listOf(Item(3, "three"), Item(1, "one"))

        assertEquals(items, items.mergeDistinct(emptyList()) { it.id })
        assertEquals(items, emptyList<Item>().mergeDistinct(items) { it.id })
        assertEquals(emptyList<Item>(), emptyList<Item>().mergeDistinct(emptyList()) { it.id })
    }

    @Test
    fun postAppendPreservesIncomingDuplicatesAndOriginalSnapshot() {
        val existing = Item(1, "existing")
        val old = persistentListOf(existing, Item(1, "existing duplicate"))
        val incoming = listOf(Item(1, "replacement"), Item(2, "first"), Item(2, "second"))

        val result = old.appendNotInOriginal(incoming) { it.id }

        assertEquals(old + incoming.filterNot { item -> old.any { it.id == item.id } }, result)
        assertEquals(listOf(1L, 1L, 2L, 2L), result.map { it.id })
        assertEquals(2, old.size)
        assertSame(existing, result.first())
    }
}
