package com.docscanner.data

import android.graphics.Bitmap
import com.docscanner.common.exceptions.ExportException
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import com.docscanner.domain.usecase.ExportPdfUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.IOException

class ExportPdfUseCaseTest {

    @Test
    fun `returns file from repository on success`() = runBlocking {
        val expected = File("/cache/export_123.pdf")
        val useCase = ExportPdfUseCase(SuccessRepo(expected))
        val result = useCase("doc-id")
        assertEquals(expected, result)
    }

    @Test
    fun `wraps IOException as ExportException`() = runBlocking {
        val useCase = ExportPdfUseCase(ThrowingRepo(IOException("disk full")))
        try {
            useCase("doc-id")
            fail("Expected ExportException")
        } catch (e: ExportException) { /* pass */ }
    }

    @Test
    fun `passes documentId to repository`() = runBlocking {
        val repo = SuccessRepo(File("/out.pdf"))
        ExportPdfUseCase(repo)("my-doc-123")
        assertEquals("my-doc-123", repo.lastExportedId)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class SuccessRepo(private val file: File) : FakeRepoBase() {
        var lastExportedId: String? = null
        override suspend fun exportPdf(documentId: String): File {
            lastExportedId = documentId
            return file
        }
    }

    private class ThrowingRepo(private val ex: Exception) : FakeRepoBase() {
        override suspend fun exportPdf(documentId: String): File = throw ex
    }

    private abstract class FakeRepoBase : com.docscanner.data.repository.DocumentRepository {
        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document? = null
        override suspend fun createDocument(name: String, bitmap: Bitmap): Document = Document("", name, 0, 0, 0, null)
        override suspend fun addPage(id: String, bitmap: Bitmap): Page = Page("", id, 1, "", 0)
        override suspend fun updatePage(id: String, page: Page, bitmap: Bitmap): Page = page
        override suspend fun reorderPages(id: String, pages: List<Page>) {}
        override suspend fun deleteDocument(id: String) {}
        override suspend fun deletePage(id: String, pageId: String) {}
        override suspend fun renameDocument(id: String, name: String) {}
        override suspend fun getDocumentCount(): Int = 0
        override suspend fun getPageCount(id: String): Int = 0
        override suspend fun getPagesForDocument(id: String): List<Page> = emptyList()
        override suspend fun exportPdf(documentId: String): File = File("/out.pdf")
        override suspend fun pageFileExists(path: String): Boolean = true
    }
}
