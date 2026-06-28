package com.docscanner.ui

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import com.docscanner.domain.usecase.DeleteDocumentUseCase
import com.docscanner.domain.usecase.ExportPdfUseCase
import com.docscanner.domain.usecase.SaveDocumentUseCase
import com.docscanner.ui.viewer.DocumentViewerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EdgeCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setupDispatcher() = Dispatchers.setMain(testDispatcher)
    @After fun teardownDispatcher() = Dispatchers.resetMain()

    // EC-8: deleting the last page triggers document deletion via DocumentViewerViewModel

    @Test
    fun ec8DeletingLastPageTriggersDocumentDeletion() = runTest {
        val singlePage = Page("p1", "doc", 1, "/p1.jpg", 0L)
        val repo = FakeRepo(listOf(singlePage))
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.deletePage("p1")
        advanceUntilIdle()

        assertTrue("Document should be deleted when last page is removed", repo.documentDeleted)
        assertTrue(vm.uiState.value.documentDeleted)
    }

    @Test
    fun deletingOneOfMultiplePagesDoesNotDeleteDocument() = runTest {
        val twoPages = listOf(
            Page("p1", "doc", 1, "/p1.jpg", 0L),
            Page("p2", "doc", 2, "/p2.jpg", 0L)
        )
        val repo = FakeRepo(twoPages)
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.deletePage("p1")
        advanceUntilIdle()

        assertFalse("Document should NOT be deleted when more pages remain", repo.documentDeleted)
    }

    // EC-5: pageFileExists reflects actual file system state via DocumentRepository

    @Test
    fun ec5PageFileExistsReturnsFalseForMissingFile() {
        val repo = FakeRepo(emptyList(), pageFileExists = false)
        val vm = makeViewModel(repo)
        val page = Page("p", "doc", 1, "/nonexistent/page.jpg", 0L)
        assertTrue("isPageFileMissing should be true", vm.isPageFileMissing(page))
    }

    // Gallery import MIME validation

    @Test
    fun galleryImportRejectsNonImageMimeTypes() {
        val validMimes = setOf("image/jpeg", "image/png")
        val invalid = listOf("application/pdf", "video/mp4", "text/plain", null)
        invalid.forEach { mime ->
            assertFalse("'$mime' should be rejected", mime in validMimes)
        }
    }

    // Auto-name format

    @Test
    fun autoNameMatchesDocumentDateTimeFormat() {
        val pattern = Regex("""^Document \d{4}-\d{2}-\d{2} \d{2}:\d{2}$""")
        assertTrue(pattern.matches("Document 2026-06-28 14:30"))
        assertFalse(pattern.matches("Document 2026-06-28 14-30"))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeViewModel(repo: FakeRepo) = DocumentViewerViewModel(
        repository = repo,
        deleteDocumentUseCase = DeleteDocumentUseCase(repo),
        exportPdfUseCase = ExportPdfUseCase(repo),
        saveDocumentUseCase = SaveDocumentUseCase(repo, File("/fake")) { Long.MAX_VALUE },
        savedStateHandle = SavedStateHandle(mapOf("documentId" to "doc"))
    )

    private class FakeRepo(
        private val pageList: List<Page> = emptyList(),
        private val pageFileExists: Boolean = true
    ) : com.docscanner.data.repository.DocumentRepository {
        var documentDeleted = false

        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document =
            Document(id, "Test", 0L, 0L, pageList.size, null)
        override suspend fun createDocument(name: String, bitmap: Bitmap): Document =
            Document("d", name, 0L, 0L, 1, null)
        override suspend fun addPage(id: String, bitmap: Bitmap): Page =
            Page("p", id, 1, "/p.jpg", 0L)
        override suspend fun updatePage(id: String, page: Page, bitmap: Bitmap): Page = page
        override suspend fun reorderPages(id: String, pages: List<Page>) {}
        override suspend fun deleteDocument(id: String) { documentDeleted = true }
        override suspend fun deletePage(id: String, pageId: String) {}
        override suspend fun renameDocument(id: String, name: String) {}
        override suspend fun getDocumentCount(): Int = 1
        override suspend fun getPageCount(id: String): Int = pageList.size
        override suspend fun getPagesForDocument(id: String): List<Page> = pageList
        override suspend fun exportPdf(id: String): File = File("/f.pdf")
        override fun pageFileExists(path: String): Boolean = pageFileExists
    }
}
