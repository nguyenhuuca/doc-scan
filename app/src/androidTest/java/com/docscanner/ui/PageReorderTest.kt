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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PageReorderTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setupDispatcher() = Dispatchers.setMain(testDispatcher)
    @After fun teardownDispatcher() = Dispatchers.resetMain()

    private val pages = listOf(
        Page("p1", "doc", 1, "/p1.jpg", 0L),
        Page("p2", "doc", 2, "/p2.jpg", 0L),
        Page("p3", "doc", 3, "/p3.jpg", 0L)
    )

    @Test
    fun reorderPagesPassesReorderedListToRepository() = runTest {
        val repo = FakeRepo(pages)
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        val reordered = listOf(pages[2], pages[0], pages[1])
        vm.reorderPages(reordered)
        advanceUntilIdle()

        assertNotNull(repo.lastReorderCall)
        assertEquals("p3", repo.lastReorderCall!![0].id)
        assertEquals("p1", repo.lastReorderCall!![1].id)
        assertEquals("p2", repo.lastReorderCall!![2].id)
    }

    @Test
    fun reorderPagesPreservesAllPages() = runTest {
        val repo = FakeRepo(pages)
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.reorderPages(pages.reversed())
        advanceUntilIdle()

        assertEquals(3, repo.lastReorderCall?.size)
    }

    @Test
    fun reorderPagesCallsRepositoryExactlyOnce() = runTest {
        val repo = FakeRepo(pages)
        val vm = makeViewModel(repo)
        advanceUntilIdle()

        vm.reorderPages(pages)
        advanceUntilIdle()

        assertEquals(1, repo.reorderCallCount)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeViewModel(repo: FakeRepo) = DocumentViewerViewModel(
        repository = repo,
        deleteDocumentUseCase = DeleteDocumentUseCase(repo),
        exportPdfUseCase = ExportPdfUseCase(repo),
        saveDocumentUseCase = SaveDocumentUseCase(repo, File("/fake")) { Long.MAX_VALUE },
        savedStateHandle = SavedStateHandle(mapOf("documentId" to "doc"))
    )

    private class FakeRepo(private val pageList: List<Page> = emptyList()) :
        com.docscanner.data.repository.DocumentRepository {
        var lastReorderCall: List<Page>? = null
        var reorderCallCount = 0

        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document =
            Document(id, "Test", 0L, 0L, pageList.size, null)
        override suspend fun createDocument(name: String, bitmap: Bitmap): Document =
            Document("d", name, 0L, 0L, 1, null)
        override suspend fun addPage(id: String, bitmap: Bitmap): Page =
            Page("p", id, 1, "/p.jpg", 0L)
        override suspend fun updatePage(id: String, page: Page, bitmap: Bitmap): Page = page
        override suspend fun reorderPages(id: String, reorderedPages: List<Page>) {
            lastReorderCall = reorderedPages
            reorderCallCount++
        }
        override suspend fun deleteDocument(id: String) {}
        override suspend fun deletePage(id: String, pageId: String) {}
        override suspend fun renameDocument(id: String, name: String) {}
        override suspend fun getDocumentCount(): Int = 1
        override suspend fun getPageCount(id: String): Int = pageList.size
        override suspend fun getPagesForDocument(id: String): List<Page> = pageList
        override suspend fun exportPdf(id: String): File = File("/f.pdf")
        override suspend fun pageFileExists(path: String): Boolean = true
    }
}
