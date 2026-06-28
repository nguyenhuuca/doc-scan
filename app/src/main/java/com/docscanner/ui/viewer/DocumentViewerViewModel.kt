package com.docscanner.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import com.docscanner.domain.usecase.DeleteDocumentUseCase
import com.docscanner.domain.usecase.ExportPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

data class DocumentViewerUiState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val canAddPage: Boolean = false,
    val exportedFile: File? = null,
    val documentDeleted: Boolean = false,
    val errorMessage: String? = null
)

class DocumentViewerViewModel(
    private val repository: DocumentRepository,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val exportPdfUseCase: ExportPdfUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val MAX_PAGES = 50
    }

    val documentId: String = savedStateHandle["documentId"] ?: ""

    private val _uiState = MutableStateFlow(DocumentViewerUiState())
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            runCatching {
                val document = repository.getDocumentById(documentId)
                val pages = repository.getPagesForDocument(documentId)
                document to pages
            }.onSuccess { (document, pages) ->
                _uiState.update {
                    it.copy(
                        document = document,
                        pages = pages,
                        isLoading = false,
                        canAddPage = pages.size < MAX_PAGES
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = "Failed to load document.")
                }
            }
        }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch {
            val pages = _uiState.value.pages
            if (pages.size <= 1) {
                // EC-8: deleting last page → delete whole document
                runCatching { deleteDocumentUseCase(documentId) }
                    .onSuccess { _uiState.update { it.copy(documentDeleted = true) } }
                    .onFailure { _uiState.update { it.copy(errorMessage = "Failed to delete document.") } }
            } else {
                runCatching { repository.deletePage(documentId, pageId) }
                    .onSuccess { loadDocument() }
                    .onFailure { _uiState.update { it.copy(errorMessage = "Failed to delete page.") } }
            }
        }
    }

    fun reorderPages(reorderedPages: List<Page>) {
        viewModelScope.launch {
            runCatching { repository.reorderPages(documentId, reorderedPages) }
                .onSuccess { loadDocument() }
                .onFailure { _uiState.update { it.copy(errorMessage = "Failed to reorder pages.") } }
        }
    }

    fun exportPdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            runCatching { exportPdfUseCase(documentId) }
                .onSuccess { file ->
                    _uiState.update { it.copy(isExporting = false, exportedFile = file) }
                }
                .onFailure {
                    _uiState.update { it.copy(isExporting = false, errorMessage = "Export failed. Please try again.") }
                }
        }
    }

    fun exportPageAsImage(page: Page): File? {
        // Returns the existing page image file for sharing (FR-11)
        val file = java.io.File(page.imagePath)
        return if (file.exists()) file else null
    }

    fun isPageFileMissing(page: Page): Boolean = !repository.pageFileExists(page.imagePath)

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearExportedFile() { _uiState.update { it.copy(exportedFile = null) } }

    fun importFromGallery(uri: Uri, context: Context) {
        viewModelScope.launch {
            val pageCount = _uiState.value.pages.size
            if (pageCount >= 50) {
                _uiState.update { it.copy(errorMessage = "Page limit (50) reached. Cannot add more pages.") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType != "image/jpeg" && mimeType != "image/png") {
                    error("Only JPEG and PNG images are supported.")
                }
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    } ?: error("Failed to decode image.")
                }
                repository.addPage(documentId, bitmap)
            }.onSuccess {
                loadDocument()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to import image.") }
            }
        }
    }

    fun copyPageToExportCache(page: Page, context: Context): File? {
        return runCatching {
            val exportDir = File(context.cacheDir, "export").also { it.mkdirs() }
            val dest = File(exportDir, "page_${UUID.randomUUID()}.jpg")
            File(page.imagePath).copyTo(dest, overwrite = true)
            dest
        }.getOrNull()
    }
}
