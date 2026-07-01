package com.docscanner.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.docscanner.R
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import com.docscanner.domain.usecase.DeleteDocumentUseCase
import com.docscanner.domain.usecase.ExportPdfUseCase
import com.docscanner.domain.usecase.GetDocumentsUseCase
import com.docscanner.domain.usecase.RenameDocumentUseCase
import com.docscanner.ui.documentlist.DocumentListScreen
import com.docscanner.ui.documentlist.DocumentListViewModel
import com.docscanner.ui.theme.DocScannerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.File

class DocumentListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun makeRepo(
        documents: List<Document> = emptyList(),
        docCount: Int = 0
    ) = object : com.docscanner.data.repository.DocumentRepository {
        override fun getAllDocuments(): Flow<List<Document>> = flowOf(documents)
        override suspend fun getDocumentById(id: String): Document? = null
        override suspend fun createDocument(n: String, b: Bitmap): Document = Document("d", n, 0L, 0L, 1, null)
        override suspend fun addPage(id: String, b: Bitmap): Page = Page("p", id, 1, "/p.jpg", 0L)
        override suspend fun addPages(id: String, bitmaps: List<Bitmap>): List<Page> =
            bitmaps.mapIndexed { i, _ -> Page("p$i", id, i + 1, "/p$i.jpg", 0L) }
        override suspend fun updatePage(id: String, p: Page, b: Bitmap): Page = p
        override suspend fun reorderPages(id: String, pages: List<Page>) {}
        override suspend fun deleteDocument(id: String) {}
        override suspend fun deletePage(id: String, pageId: String) {}
        override suspend fun renameDocument(id: String, name: String) {}
        override suspend fun getDocumentCount(): Int = docCount
        override suspend fun getPageCount(id: String): Int = 0
        override suspend fun getPagesForDocument(id: String): List<Page> = emptyList()
        override suspend fun exportPdf(id: String): File = File("/f.pdf")
        override suspend fun pageFileExists(path: String): Boolean = true
    }

    @Test
    fun emptyStateShowsPlaceholderText() {
        val repo = makeRepo(emptyList())
        val vm = DocumentListViewModel(
            GetDocumentsUseCase(repo),
            DeleteDocumentUseCase(repo),
            RenameDocumentUseCase(repo),
            ExportPdfUseCase(repo)
        )
        composeTestRule.setContent {
            DocScannerTheme {
                DocumentListScreen(
                    viewModel = vm,
                    onNavigateToScanner = {},
                    onNavigateToViewer = {},
                    onNavigateToSettings = {}
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_documents_yet)).assertIsDisplayed()
    }

    @Test
    fun documentCardShowsDocumentName() {
        val doc = Document("id1", "My Test Document", System.currentTimeMillis(), System.currentTimeMillis(), 3, null)
        val repo = makeRepo(listOf(doc), 1)
        val vm = DocumentListViewModel(
            GetDocumentsUseCase(repo),
            DeleteDocumentUseCase(repo),
            RenameDocumentUseCase(repo),
            ExportPdfUseCase(repo)
        )
        composeTestRule.setContent {
            DocScannerTheme {
                DocumentListScreen(
                    viewModel = vm,
                    onNavigateToScanner = {},
                    onNavigateToViewer = {},
                    onNavigateToSettings = {}
                )
            }
        }
        composeTestRule.onNodeWithText("My Test Document").assertIsDisplayed()
    }
}
