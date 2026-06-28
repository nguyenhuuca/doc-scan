package com.docscanner.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import com.docscanner.ui.edit.EditScreen
import com.docscanner.ui.edit.EditViewModel
import com.docscanner.ui.theme.DocScannerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.File

class EditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testPage = Page("p1", "doc1", 0, "/fake/page.jpg", 0L)
    private val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    private fun makeRepo() = object : com.docscanner.data.repository.DocumentRepository {
        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document? = null
        override suspend fun createDocument(n: String, b: Bitmap): Document = Document("d", n, 0L, 0L, 1, null)
        override suspend fun addPage(id: String, b: Bitmap): Page = Page("p", id, 1, "/p.jpg", 0L)
        override suspend fun updatePage(id: String, p: Page, b: Bitmap): Page = p
        override suspend fun reorderPages(id: String, pages: List<Page>) {}
        override suspend fun deleteDocument(id: String) {}
        override suspend fun deletePage(id: String, pageId: String) {}
        override suspend fun renameDocument(id: String, name: String) {}
        override suspend fun getDocumentCount(): Int = 0
        override suspend fun getPageCount(id: String): Int = 1
        override suspend fun getPagesForDocument(id: String): List<Page> = listOf(testPage)
        override suspend fun exportPdf(id: String): File = File("/f.pdf")
        override fun pageFileExists(path: String): Boolean = false
    }

    @Test
    fun editScreenShowsPageTitle() {
        val savedState = SavedStateHandle(mapOf("documentId" to "doc1", "pageIndex" to 0))
        val vm = EditViewModel(makeRepo(), savedState)
        composeTestRule.setContent {
            DocScannerTheme { EditScreen(viewModel = vm, onNavigateBack = {}) }
        }
        composeTestRule.onNodeWithText("Edit Page 1").assertIsDisplayed()
    }

    @Test
    fun saveButtonDisabledWithNoChanges() {
        val savedState = SavedStateHandle(mapOf("documentId" to "doc1", "pageIndex" to 0))
        val vm = EditViewModel(makeRepo(), savedState)
        composeTestRule.setContent {
            DocScannerTheme { EditScreen(viewModel = vm, onNavigateBack = {}) }
        }
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun undoButtonDisabledInitially() {
        val savedState = SavedStateHandle(mapOf("documentId" to "doc1", "pageIndex" to 0))
        val vm = EditViewModel(makeRepo(), savedState)
        composeTestRule.setContent {
            DocScannerTheme { EditScreen(viewModel = vm, onNavigateBack = {}) }
        }
        composeTestRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()
    }
}
