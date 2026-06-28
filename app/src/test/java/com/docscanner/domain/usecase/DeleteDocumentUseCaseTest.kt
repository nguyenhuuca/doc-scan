package com.docscanner.domain.usecase

import android.graphics.Bitmap
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class DeleteDocumentUseCaseTest {

    @Test
    fun `invoke calls repository deleteDocument with the given id`() = runBlocking {
        val repo = FakeRepo()
        DeleteDocumentUseCase(repo)("doc-abc")
        assertEquals("doc-abc", repo.deletedId)
    }

    @Test
    fun `invoke does not throw when document does not exist`() = runBlocking {
        val repo = FakeRepo(throwOnDelete = false)
        // Should complete without exception
        DeleteDocumentUseCase(repo)("nonexistent")
        assertEquals("nonexistent", repo.deletedId)
    }

    @Test
    fun `invoke is called exactly once per invocation`() = runBlocking {
        val repo = FakeRepo()
        DeleteDocumentUseCase(repo)("doc-1")
        assertEquals(1, repo.deleteCallCount)
    }

    // ── Fake ──────────────────────────────────────────────────────────────────

    private class FakeRepo(private val throwOnDelete: Boolean = false) :
        com.docscanner.data.repository.DocumentRepository {
        var deletedId: String? = null
        var deleteCallCount = 0

        override suspend fun deleteDocument(documentId: String) {
            deleteCallCount++
            if (throwOnDelete) throw RuntimeException("Already deleted")
            deletedId = documentId
        }

        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document? = null
        override suspend fun createDocument(name: String, bitmap: Bitmap): Document = Document("", name, 0, 0, 0, null)
        override suspend fun addPage(id: String, bitmap: Bitmap): Page = Page("", id, 1, "", 0)
        override suspend fun updatePage(id: String, page: Page, bitmap: Bitmap): Page = page
        override suspend fun reorderPages(id: String, pages: List<Page>) {}
        override suspend fun deletePage(id: String, pageId: String) {}
        override suspend fun renameDocument(id: String, name: String) {}
        override suspend fun getDocumentCount(): Int = 0
        override suspend fun getPageCount(id: String): Int = 0
        override suspend fun getPagesForDocument(id: String): List<Page> = emptyList()
        override suspend fun exportPdf(id: String): File = File("/fake.pdf")
        override suspend fun pageFileExists(path: String): Boolean = true
    }
}
