package com.docscanner.domain.usecase

import com.docscanner.common.exceptions.DocumentLimitException
import com.docscanner.common.exceptions.PageLimitException
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SaveDocumentUseCaseTest {

    // Fake repository for testing
    private class FakeRepository(
        private val documentCount: Int = 0,
        private val pageCount: Int = 0
    ) : com.docscanner.data.repository.DocumentRepository {
        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(documentId: String): Document? = null
        override suspend fun createDocument(name: String, firstPageBitmap: android.graphics.Bitmap): Document =
            Document("test-id", name, 0L, 0L, 1, null)
        override suspend fun addPage(documentId: String, bitmap: android.graphics.Bitmap): Page =
            Page("page-id", documentId, pageCount + 1, "/fake/path", 0L)
        override suspend fun updatePage(documentId: String, page: Page, newBitmap: android.graphics.Bitmap): Page = page
        override suspend fun reorderPages(documentId: String, reorderedPages: List<Page>) {}
        override suspend fun deleteDocument(documentId: String) {}
        override suspend fun deletePage(documentId: String, pageId: String) {}
        override suspend fun renameDocument(documentId: String, newName: String) {}
        override suspend fun getDocumentCount(): Int = documentCount
        override suspend fun getPageCount(documentId: String): Int = pageCount
        override suspend fun getPagesForDocument(documentId: String): List<Page> = emptyList()
        override suspend fun exportPdf(documentId: String): java.io.File = java.io.File("/fake.pdf")
        override fun pageFileExists(imagePath: String): Boolean = true
    }

    @Test
    fun `createDocument throws DocumentLimitException when 100 documents exist`() = runBlocking {
        val useCase = SaveDocumentUseCase(FakeRepository(documentCount = 100), File("/fake"))
        try {
            useCase.createDocument(android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888))
            fail("Expected DocumentLimitException")
        } catch (e: DocumentLimitException) {
            // pass
        }
    }

    @Test
    fun `addPage throws PageLimitException when 50 pages exist`() = runBlocking {
        val useCase = SaveDocumentUseCase(FakeRepository(pageCount = 50), File("/fake"))
        try {
            useCase.addPage("doc-id", android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888))
            fail("Expected PageLimitException")
        } catch (e: PageLimitException) {
            // pass
        }
    }
}
