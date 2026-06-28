package com.docscanner.domain.usecase

import android.graphics.Bitmap
import com.docscanner.common.exceptions.DocumentLimitException
import com.docscanner.common.exceptions.PageLimitException
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SaveDocumentUseCaseTest {

    private val plentifulStorage: (File) -> Long = { Long.MAX_VALUE }
    private val emptyStorage: (File) -> Long = { 0L }

    private fun makeUseCase(
        docCount: Int = 0,
        pageCount: Int = 0,
        storage: (File) -> Long = plentifulStorage
    ) = SaveDocumentUseCase(FakeRepo(docCount, pageCount), File("/fake"), storage)

    // ── Limit enforcement ─────────────────────────────────────────────────────

    @Test
    fun `createDocument throws DocumentLimitException at 100 documents`() = runBlocking {
        try {
            makeUseCase(docCount = 100).createDocument(fakeBitmap())
            fail("Expected DocumentLimitException")
        } catch (e: DocumentLimitException) { /* pass */ }
    }

    @Test
    fun `createDocument succeeds at 99 documents`() = runBlocking {
        val doc = makeUseCase(docCount = 99).createDocument(fakeBitmap())
        assertNotNull(doc)
    }

    @Test
    fun `addPage throws PageLimitException at 50 pages`() = runBlocking {
        try {
            makeUseCase(pageCount = 50).addPage("doc", fakeBitmap())
            fail("Expected PageLimitException")
        } catch (e: PageLimitException) { /* pass */ }
    }

    @Test
    fun `addPage succeeds at 49 pages`() = runBlocking {
        val page = makeUseCase(pageCount = 49).addPage("doc", fakeBitmap())
        assertNotNull(page)
    }

    // ── Storage check ─────────────────────────────────────────────────────────

    @Test
    fun `createDocument throws StorageFullException when storage is empty`() = runBlocking {
        try {
            makeUseCase(storage = emptyStorage).createDocument(fakeBitmap())
            fail("Expected StorageFullException")
        } catch (e: com.docscanner.common.exceptions.StorageFullException) { /* pass */ }
    }

    // ── Name format ───────────────────────────────────────────────────────────

    @Test
    fun `createDocument uses auto-generated name starting with Document`() = runBlocking {
        val repo = FakeRepo()
        SaveDocumentUseCase(repo, File("/fake"), plentifulStorage).createDocument(fakeBitmap())
        assertNotNull(repo.createdName)
        assert(repo.createdName!!.startsWith("Document ")) {
            "Expected name starting with 'Document ', got '${repo.createdName}'"
        }
    }

    @Test
    fun `addPage routes to correct document id`() = runBlocking {
        val repo = FakeRepo(pageCount = 0)
        SaveDocumentUseCase(repo, File("/fake"), plentifulStorage).addPage("my-doc", fakeBitmap())
        assertEquals("my-doc", repo.addedDocumentId)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeBitmap() = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    private class FakeRepo(
        private val docCount: Int = 0,
        private val pageCount: Int = 0
    ) : com.docscanner.data.repository.DocumentRepository {
        var createdName: String? = null
        var addedDocumentId: String? = null

        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document? = null
        override suspend fun createDocument(name: String, bitmap: Bitmap): Document {
            createdName = name
            return Document("test-id", name, 0L, 0L, 1, null)
        }
        override suspend fun addPage(documentId: String, bitmap: Bitmap): Page {
            addedDocumentId = documentId
            return Page("page-id", documentId, pageCount + 1, "/fake/path", 0L)
        }
        override suspend fun updatePage(documentId: String, page: Page, bitmap: Bitmap): Page = page
        override suspend fun reorderPages(documentId: String, pages: List<Page>) {}
        override suspend fun deleteDocument(documentId: String) {}
        override suspend fun deletePage(documentId: String, pageId: String) {}
        override suspend fun renameDocument(documentId: String, name: String) {}
        override suspend fun getDocumentCount(): Int = docCount
        override suspend fun getPageCount(documentId: String): Int = pageCount
        override suspend fun getPagesForDocument(documentId: String): List<Page> = emptyList()
        override suspend fun exportPdf(documentId: String): File = File("/fake.pdf")
        override fun pageFileExists(imagePath: String): Boolean = true
    }
}
