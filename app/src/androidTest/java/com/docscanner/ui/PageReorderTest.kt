package com.docscanner.ui

import android.graphics.Bitmap
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class PageReorderTest {

    private val pages = listOf(
        Page("p1", "doc", 1, "/p1.jpg", 0L),
        Page("p2", "doc", 2, "/p2.jpg", 0L),
        Page("p3", "doc", 3, "/p3.jpg", 0L)
    )

    private class FakeRepo : com.docscanner.data.repository.DocumentRepository {
        val reorderCalls = mutableListOf<List<Page>>()
        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document? =
            Document(id, "Test", 0L, 0L, 3, null)
        override suspend fun createDocument(name: String, bitmap: Bitmap): Document =
            Document("d", name, 0L, 0L, 1, null)
        override suspend fun addPage(documentId: String, bitmap: Bitmap): Page =
            Page("p", documentId, 1, "/p.jpg", 0L)
        override suspend fun updatePage(documentId: String, page: Page, newBitmap: Bitmap): Page = page
        override suspend fun reorderPages(documentId: String, reorderedPages: List<Page>) {
            reorderCalls.add(reorderedPages)
        }
        override suspend fun deleteDocument(documentId: String) {}
        override suspend fun deletePage(documentId: String, pageId: String) {}
        override suspend fun renameDocument(documentId: String, newName: String) {}
        override suspend fun getDocumentCount(): Int = 1
        override suspend fun getPageCount(documentId: String): Int = 3
        override suspend fun getPagesForDocument(documentId: String): List<Page> = pages
        override suspend fun exportPdf(documentId: String): File = File("/f.pdf")
        override fun pageFileExists(imagePath: String): Boolean = true
    }

    @Test
    fun `reorder calls repository with reordered list`() = runBlocking {
        val repo = FakeRepo()
        val reordered = listOf(pages[2], pages[0], pages[1])
        repo.reorderPages("doc", reordered)
        assertEquals(1, repo.reorderCalls.size)
        assertEquals("p3", repo.reorderCalls[0][0].id)
        assertEquals("p1", repo.reorderCalls[0][1].id)
        assertEquals("p2", repo.reorderCalls[0][2].id)
    }

    @Test
    fun `reorder preserves all pages`() = runBlocking {
        val repo = FakeRepo()
        val reordered = pages.reversed()
        repo.reorderPages("doc", reordered)
        assertEquals(3, repo.reorderCalls[0].size)
    }
}
