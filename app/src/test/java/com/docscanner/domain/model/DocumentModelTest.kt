package com.docscanner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentModelTest {

    private fun doc(
        id: String = "id-1",
        name: String = "Test Doc",
        createdAt: Long = 1000L,
        updatedAt: Long = 2000L,
        pageCount: Int = 3,
        thumbnailPath: String? = "/thumb/path.jpg"
    ) = Document(id, name, createdAt, updatedAt, pageCount, thumbnailPath)

    // ── Document equals ───────────────────────────────────────────────────────

    @Test
    fun `two documents with identical fields are equal`() {
        val a = doc()
        val b = doc()
        assertEquals(a, b)
    }

    @Test
    fun `documents with different id are not equal`() {
        assertNotEquals(doc(id = "a"), doc(id = "b"))
    }

    @Test
    fun `documents with different name are not equal`() {
        assertNotEquals(doc(name = "Alpha"), doc(name = "Beta"))
    }

    @Test
    fun `documents with null and non-null thumbnailPath are not equal`() {
        assertNotEquals(doc(thumbnailPath = null), doc(thumbnailPath = "/some/path.jpg"))
    }

    @Test
    fun `two documents with null thumbnailPath are equal`() {
        assertEquals(doc(thumbnailPath = null), doc(thumbnailPath = null))
    }

    // ── Document hashCode ─────────────────────────────────────────────────────

    @Test
    fun `equal documents have same hashCode`() {
        assertEquals(doc().hashCode(), doc().hashCode())
    }

    @Test
    fun `document with null thumbnailPath has consistent hashCode`() {
        val d = doc(thumbnailPath = null)
        assertEquals(d.hashCode(), d.hashCode())
    }

    // ── Document copy ─────────────────────────────────────────────────────────

    @Test
    fun `copy with new name changes only name`() {
        val original = doc()
        val copied = original.copy(name = "New Name")
        assertEquals("New Name", copied.name)
        assertEquals(original.id, copied.id)
        assertEquals(original.pageCount, copied.pageCount)
    }

    @Test
    fun `copy with null thumbnailPath clears thumbnail`() {
        val copied = doc(thumbnailPath = "/some/thumb.jpg").copy(thumbnailPath = null)
        assertEquals(null, copied.thumbnailPath)
    }

    // ── Document toString ─────────────────────────────────────────────────────

    @Test
    fun `toString contains id and name`() {
        val d = doc(id = "abc-123", name = "My Doc")
        val s = d.toString()
        assertTrue("toString should contain id", s.contains("abc-123"))
        assertTrue("toString should contain name", s.contains("My Doc"))
    }

    // ── Document component functions ──────────────────────────────────────────

    @Test
    fun `destructuring returns fields in declaration order`() {
        val d = doc(id = "x", name = "y", createdAt = 1L, updatedAt = 2L, pageCount = 5, thumbnailPath = "/t")
        val (id, name, createdAt, updatedAt, pageCount, thumbnailPath) = d
        assertEquals("x", id)
        assertEquals("y", name)
        assertEquals(1L, createdAt)
        assertEquals(2L, updatedAt)
        assertEquals(5, pageCount)
        assertEquals("/t", thumbnailPath)
    }
}

class PageModelTest {

    private fun page(
        id: String = "page-1",
        documentId: String = "doc-1",
        pageNumber: Int = 1,
        imagePath: String = "/images/page_001.jpg",
        createdAt: Long = 1000L
    ) = Page(id, documentId, pageNumber, imagePath, createdAt)

    // ── Page equals ───────────────────────────────────────────────────────────

    @Test
    fun `two pages with identical fields are equal`() {
        assertEquals(page(), page())
    }

    @Test
    fun `pages with different pageNumber are not equal`() {
        assertNotEquals(page(pageNumber = 1), page(pageNumber = 2))
    }

    @Test
    fun `pages with different imagePath are not equal`() {
        assertNotEquals(page(imagePath = "/a.jpg"), page(imagePath = "/b.jpg"))
    }

    // ── Page hashCode ─────────────────────────────────────────────────────────

    @Test
    fun `equal pages have same hashCode`() {
        assertEquals(page().hashCode(), page().hashCode())
    }

    // ── Page copy ─────────────────────────────────────────────────────────────

    @Test
    fun `copy with new pageNumber changes only pageNumber`() {
        val original = page()
        val copied = original.copy(pageNumber = 5)
        assertEquals(5, copied.pageNumber)
        assertEquals(original.id, copied.id)
        assertEquals(original.documentId, copied.documentId)
    }

    // ── Page toString ─────────────────────────────────────────────────────────

    @Test
    fun `toString contains id and documentId`() {
        val p = page(id = "pg-99", documentId = "doc-77")
        val s = p.toString()
        assertTrue("toString should contain id", s.contains("pg-99"))
        assertTrue("toString should contain documentId", s.contains("doc-77"))
    }

    // ── Page component functions ──────────────────────────────────────────────

    @Test
    fun `destructuring returns fields in declaration order`() {
        val p = page(id = "p", documentId = "d", pageNumber = 2, imagePath = "/img.jpg", createdAt = 5L)
        val (id, documentId, pageNumber, imagePath, createdAt) = p
        assertEquals("p", id)
        assertEquals("d", documentId)
        assertEquals(2, pageNumber)
        assertEquals("/img.jpg", imagePath)
        assertEquals(5L, createdAt)
    }
}
