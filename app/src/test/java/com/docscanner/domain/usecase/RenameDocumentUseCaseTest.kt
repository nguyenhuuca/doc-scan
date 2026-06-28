package com.docscanner.domain.usecase

import com.docscanner.common.exceptions.DocumentNameException
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class RenameDocumentUseCaseTest {

    private val doc = Document("id", "Original Name", 0L, 0L, 1, null)

    private inner class FakeRepo(private val document: Document = doc) : DocumentRepository {
        var lastRenamedTo: String? = null
        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(documentId: String): Document = document
        override suspend fun createDocument(name: String, firstPageBitmap: android.graphics.Bitmap): Document = document
        override suspend fun addPage(documentId: String, bitmap: android.graphics.Bitmap): Page = Page("", "", 1, "", 0L)
        override suspend fun updatePage(documentId: String, page: Page, newBitmap: android.graphics.Bitmap): Page = page
        override suspend fun reorderPages(documentId: String, reorderedPages: List<Page>) {}
        override suspend fun deleteDocument(documentId: String) {}
        override suspend fun deletePage(documentId: String, pageId: String) {}
        override suspend fun renameDocument(documentId: String, newName: String) { lastRenamedTo = newName }
        override suspend fun getDocumentCount(): Int = 0
        override suspend fun getPageCount(documentId: String): Int = 0
        override suspend fun getPagesForDocument(documentId: String): List<Page> = emptyList()
        override suspend fun exportPdf(documentId: String): File = File("/fake.pdf")
        override fun pageFileExists(imagePath: String): Boolean = true
    }

    @Test
    fun `rename with valid name succeeds`() = runBlocking {
        val repo = FakeRepo()
        RenameDocumentUseCase(repo)("id", "New Name")
        assertEquals("New Name", repo.lastRenamedTo)
    }

    @Test
    fun `rename with same name skips write EC-7`() = runBlocking {
        val repo = FakeRepo()
        RenameDocumentUseCase(repo)("id", "Original Name")
        assertEquals(null, repo.lastRenamedTo) // No write
    }

    @Test
    fun `rename with empty name throws DocumentNameException`() = runBlocking {
        try {
            RenameDocumentUseCase(FakeRepo())("id", "   ")
            fail()
        } catch (e: DocumentNameException) { }
    }

    @Test
    fun `rename with 51 chars throws DocumentNameException`() = runBlocking {
        try {
            RenameDocumentUseCase(FakeRepo())("id", "a".repeat(51))
            fail()
        } catch (e: DocumentNameException) { }
    }

    @Test
    fun `rename with exactly 50 chars succeeds`() = runBlocking {
        val repo = FakeRepo()
        RenameDocumentUseCase(repo)("id", "a".repeat(50))
        assertEquals("a".repeat(50), repo.lastRenamedTo)
    }

    @Test
    fun `rename with forbidden char throws DocumentNameException`() = runBlocking {
        val forbidden = listOf("/", "\\", ":", "*", "?", "\"", "<", ">", "|")
        val repo = FakeRepo()
        forbidden.forEach { char ->
            try {
                RenameDocumentUseCase(repo)("id", "name${char}test")
                fail("Expected exception for char: $char")
            } catch (e: DocumentNameException) { }
        }
    }

    @Test
    fun `rename strips leading and trailing whitespace`() = runBlocking {
        val repo = FakeRepo()
        RenameDocumentUseCase(repo)("id", "  New Name  ")
        assertEquals("New Name", repo.lastRenamedTo)
    }
}
