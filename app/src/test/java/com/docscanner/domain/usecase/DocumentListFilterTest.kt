package com.docscanner.domain.usecase

import com.docscanner.domain.model.Document
import com.docscanner.ui.documentlist.SortOrder
import com.docscanner.ui.documentlist.filterAndSort
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentListFilterTest {

    private fun doc(id: String, name: String, updatedAt: Long = 0L) = Document(
        id = id, name = name, createdAt = 0L, updatedAt = updatedAt, pageCount = 1, thumbnailPath = null
    )

    private val docs = listOf(
        doc("1", "Zebra report", updatedAt = 3L),
        doc("2", "apple notes",  updatedAt = 1L),
        doc("3", "Banana scan",  updatedAt = 2L)
    )

    @Test
    fun emptyQueryReturnsAll() {
        assertEquals(3, filterAndSort(docs, "", SortOrder.DATE_DESC).size)
    }

    @Test
    fun queryFiltersCaseInsensitive() {
        val result = filterAndSort(docs, "ban", SortOrder.DATE_DESC)
        assertEquals(1, result.size)
        assertEquals("Banana scan", result[0].name)
    }

    @Test
    fun queryNoMatchReturnsEmpty() {
        assertEquals(0, filterAndSort(docs, "xyz", SortOrder.DATE_DESC).size)
    }

    @Test
    fun sortNameAscOrders() {
        val result = filterAndSort(docs, "", SortOrder.NAME_ASC)
        assertEquals(listOf("apple notes", "Banana scan", "Zebra report"), result.map { it.name })
    }

    @Test
    fun sortDateDescPreservesInputOrder() {
        assertEquals(docs, filterAndSort(docs, "", SortOrder.DATE_DESC))
    }

    @Test
    fun searchThenSortNameAsc() {
        // "a" matches all three: apple, Banana, Zebr-a
        val result = filterAndSort(docs, "a", SortOrder.NAME_ASC)
        assertEquals(listOf("apple notes", "Banana scan", "Zebra report"), result.map { it.name })
    }
}
