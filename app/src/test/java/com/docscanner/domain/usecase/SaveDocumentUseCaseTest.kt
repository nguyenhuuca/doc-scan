package com.docscanner.domain.usecase

import com.docscanner.common.exceptions.DocumentLimitException
import com.docscanner.common.exceptions.PageLimitException
import com.docscanner.common.exceptions.StorageFullException
import com.docscanner.data.repository.DocumentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File

// Tests business logic (limits, storage, name generation) via extracted internal methods.
// Full integration tests (routing, success paths) live in androidTest/ScannerFlowTest.
class SaveDocumentUseCaseTest {

    private val plentifulStorage: (File) -> Long = { Long.MAX_VALUE }
    private val emptyStorage: (File) -> Long = { 0L }

    private fun makeUseCase(storage: (File) -> Long = plentifulStorage) =
        SaveDocumentUseCase(mock(DocumentRepository::class.java), File("/fake"), storage)

    // ── Document count limit ──────────────────────────────────────────────────

    @Test
    fun `createDocument throws DocumentLimitException at 100 documents`() {
        try {
            makeUseCase().validateDocumentCount(SaveDocumentUseCase.MAX_DOCUMENTS)
            fail("Expected DocumentLimitException")
        } catch (e: DocumentLimitException) { /* pass */ }
    }

    @Test
    fun `createDocument does not throw at 99 documents`() {
        makeUseCase().validateDocumentCount(SaveDocumentUseCase.MAX_DOCUMENTS - 1)
        // No exception → pass
    }

    // ── Page count limit ──────────────────────────────────────────────────────

    @Test
    fun `addPage throws PageLimitException at 50 pages`() {
        try {
            makeUseCase().validatePageCount(SaveDocumentUseCase.MAX_PAGES, "doc-id")
            fail("Expected PageLimitException")
        } catch (e: PageLimitException) { /* pass */ }
    }

    @Test
    fun `addPage does not throw at 49 pages`() {
        makeUseCase().validatePageCount(SaveDocumentUseCase.MAX_PAGES - 1, "doc-id")
        // No exception → pass
    }

    // ── Storage check ─────────────────────────────────────────────────────────

    @Test
    fun `validateStorage throws StorageFullException when storage is empty`() {
        try {
            makeUseCase(emptyStorage).validateStorage()
            fail("Expected StorageFullException")
        } catch (e: StorageFullException) { /* pass */ }
    }

    @Test
    fun `validateStorage does not throw with plentiful storage`() {
        makeUseCase(plentifulStorage).validateStorage()
        // No exception → pass
    }

    // ── Name generation ───────────────────────────────────────────────────────

    @Test
    fun `buildDocumentName starts with Document`() {
        val name = makeUseCase().buildDocumentName()
        assertNotNull(name)
        assertTrue("Expected name starting with 'Document ', got '$name'", name.startsWith("Document "))
    }

    @Test
    fun `buildDocumentName matches Document YYYY-MM-DD HHmm format`() {
        val name = makeUseCase().buildDocumentName()
        val pattern = Regex("""^Document \d{4}-\d{2}-\d{2} \d{2}:\d{2}$""")
        assertTrue("Name '$name' did not match expected pattern", pattern.matches(name))
    }

    // ── Storage boundary ──────────────────────────────────────────────────────

    @Test
    fun `validateStorage throws StorageFullException one byte below threshold`() {
        val oneBelowMin: (File) -> Long = { SaveDocumentUseCase.MIN_STORAGE_BYTES - 1 }
        try {
            makeUseCase(oneBelowMin).validateStorage()
            fail("Expected StorageFullException")
        } catch (e: StorageFullException) { /* pass */ }
    }

    @Test
    fun `validateStorage does not throw exactly at threshold`() {
        val atMin: (File) -> Long = { SaveDocumentUseCase.MIN_STORAGE_BYTES }
        makeUseCase(atMin).validateStorage()
    }

    @Test
    fun `validateStorage does not throw one byte above threshold`() {
        val oneAboveMin: (File) -> Long = { SaveDocumentUseCase.MIN_STORAGE_BYTES + 1 }
        makeUseCase(oneAboveMin).validateStorage()
    }

    // ── Batch page count ──────────────────────────────────────────────────────

    @Test
    fun `validateBatchPageCount throws when batch would exceed MAX_PAGES`() {
        try {
            makeUseCase().validateBatchPageCount(
                currentCount = SaveDocumentUseCase.MAX_PAGES - 1,
                batchSize = 2,
                documentId = "doc"
            )
            fail("Expected PageLimitException")
        } catch (e: PageLimitException) { /* pass */ }
    }

    @Test
    fun `validateBatchPageCount does not throw when batch fills exactly to MAX_PAGES`() {
        makeUseCase().validateBatchPageCount(
            currentCount = SaveDocumentUseCase.MAX_PAGES - 5,
            batchSize = 5,
            documentId = "doc"
        )
    }

    @Test
    fun `validateBatchPageCount does not throw when batch stays under MAX_PAGES`() {
        makeUseCase().validateBatchPageCount(
            currentCount = 0,
            batchSize = SaveDocumentUseCase.MAX_PAGES - 1,
            documentId = "doc"
        )
    }

    @Test
    fun `validateBatchPageCount throws when current count already at MAX_PAGES`() {
        try {
            makeUseCase().validateBatchPageCount(
                currentCount = SaveDocumentUseCase.MAX_PAGES,
                batchSize = 1,
                documentId = "doc"
            )
            fail("Expected PageLimitException")
        } catch (e: PageLimitException) { /* pass */ }
    }

    // ── addPages early return ─────────────────────────────────────────────────

    @Test
    fun `addPages with empty list returns empty without touching repository`() = runBlocking {
        val result = makeUseCase().addPages("doc-id", emptyList())
        assertEquals(emptyList<com.docscanner.domain.model.Page>(), result)
    }
}
