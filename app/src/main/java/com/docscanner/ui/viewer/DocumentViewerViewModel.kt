package com.docscanner.ui.viewer

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.common.AppConfig
import com.docscanner.common.calcInSampleSize
import com.docscanner.common.exceptions.PageLimitException
import com.docscanner.common.exceptions.StorageFullException
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import com.docscanner.domain.usecase.DeleteDocumentUseCase
import com.docscanner.domain.usecase.ExportPdfUseCase
import com.docscanner.domain.usecase.SaveDocumentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class DocumentViewerUiState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val missingPageIds: Set<String> = emptySet(),
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
    private val saveDocumentUseCase: SaveDocumentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val MAX_PAGES        = AppConfig.MAX_PAGES
        private val MAX_IMPORT_WIDTH = AppConfig.IMAGE_MAX_WIDTH
        private val MAX_IMPORT_HEIGHT= AppConfig.IMAGE_MAX_HEIGHT
    }

    val documentId: String = savedStateHandle["documentId"] ?: ""

    private val _uiState = MutableStateFlow(DocumentViewerUiState())
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    private val pageMutex = Mutex()

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            runCatching {
                val document = repository.getDocumentById(documentId)
                val pages = repository.getPagesForDocument(documentId)
                val missing = withContext(Dispatchers.IO) {
                    pages.filter { !repository.pageFileExists(it.imagePath) }.map { it.id }.toSet()
                }
                Triple(document, pages, missing)
            }.onSuccess { (document, pages, missing) ->
                _uiState.update {
                    it.copy(
                        document = document,
                        pages = pages,
                        missingPageIds = missing,
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
            pageMutex.withLock {
                val pages = _uiState.value.pages
                if (pages.size <= 1) {
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
        val file = File(page.imagePath)
        return if (file.exists()) file else null
    }

    fun isPageFileMissing(page: Page): Boolean = _uiState.value.missingPageIds.contains(page.id)

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearExportedFile() { _uiState.update { it.copy(exportedFile = null) } }

    // items = list of (rawBytes, mimeType) read on the CALLING main thread immediately
    // after the gallery result — URI permission may be scoped to the delivery window.
    fun importFromGallery(items: List<Pair<ByteArray, String?>>) {
        val validBytes = items
            .filter { (_, mime) -> mime == "image/jpeg" || mime == "image/png" }
            .map { (bytes, _) -> bytes }

        if (validBytes.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Only JPEG and PNG images are supported.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val bitmaps = withContext(Dispatchers.Default) {
                    validBytes.mapNotNull { bytes ->
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        opts.inSampleSize = calcInSampleSize(opts.outWidth, opts.outHeight, MAX_IMPORT_WIDTH, MAX_IMPORT_HEIGHT)
                        opts.inJustDecodeBounds = false
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    }
                }
                if (bitmaps.isEmpty()) error("Failed to decode images.")
                bitmaps.forEach { bitmap -> saveDocumentUseCase.addPage(documentId, bitmap) }
            }.onSuccess {
                loadDocument()
            }.onFailure { e ->
                val msg = when (e) {
                    is PageLimitException -> "Page limit ($MAX_PAGES) reached."
                    is StorageFullException -> "Not enough storage to import image."
                    else -> "Failed to import image. Please try again."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    suspend fun copyPageToExportCache(page: Page, context: Context): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val exportDir = File(context.cacheDir, "export").also { it.mkdirs() }
                val dest = File(exportDir, "page_${UUID.randomUUID()}.jpg")
                File(page.imagePath).copyTo(dest, overwrite = true)
                dest
            }.getOrNull()
        }
}
