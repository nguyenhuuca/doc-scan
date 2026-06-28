package com.docscanner.ui.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class EditUiState(
    val currentBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val canUndo: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class EditViewModel(
    private val repository: DocumentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val MAX_UNDO_STACK = 5
        private const val UNDO_JPEG_QUALITY = 75
    }

    val documentId: String = savedStateHandle["documentId"] ?: ""
    val pageIndex: Int = savedStateHandle["pageIndex"] ?: 0

    private var currentPage: Page? = null
    // Bitmaps are stored as JPEG-compressed ByteArrays to limit memory.
    // 5 × ~200 KB compressed vs 5 × ~34 MB decoded — prevents OOM on high-res pages.
    private val undoStack = ArrayDeque<ByteArray>()
    private val transformMutex = Mutex()

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    init {
        loadInitialPage()
    }

    private fun loadInitialPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            runCatching {
                val pages = repository.getPagesForDocument(documentId)
                val page = pages.getOrNull(pageIndex) ?: error("Page $pageIndex not found")
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(page.imagePath)
                } ?: error("Failed to decode page image")
                page to bitmap
            }.onSuccess { (page, bitmap) ->
                currentPage = page
                _uiState.update { it.copy(currentBitmap = bitmap, isProcessing = false) }
            }.onFailure {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Failed to load page.") }
            }
        }
    }

    fun loadPage(page: Page, initialBitmap: Bitmap) {
        currentPage = page
        _uiState.update { it.copy(currentBitmap = initialBitmap, hasUnsavedChanges = false, canUndo = false) }
    }

    fun rotate() = applyTransform { bitmap -> ImageProcessor.rotateBitmap(bitmap) }

    fun adjustBrightness(value: Float) = applyTransform { bitmap -> ImageProcessor.adjustBrightness(bitmap, value) }

    fun adjustContrast(value: Float) = applyTransform { bitmap -> ImageProcessor.adjustContrast(bitmap, value) }

    fun toGrayscale() = applyTransform { bitmap -> ImageProcessor.toGrayscale(bitmap) }

    fun undo() {
        if (undoStack.isEmpty()) return
        val bytes = undoStack.removeLast()
        val previous = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val old = _uiState.value.currentBitmap
        _uiState.update {
            it.copy(
                currentBitmap = previous,
                canUndo = undoStack.isNotEmpty(),
                hasUnsavedChanges = undoStack.isNotEmpty()
            )
        }
        old?.recycle()
    }

    fun save() {
        val page = currentPage ?: return
        val bitmap = _uiState.value.currentBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            runCatching {
                repository.updatePage(documentId, page, bitmap)
            }.onSuccess {
                currentPage = it
                undoStack.clear()
                _uiState.update { state ->
                    state.copy(isProcessing = false, hasUnsavedChanges = false, isSaved = true, canUndo = false)
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isProcessing = false, errorMessage = "Failed to save changes.")
                }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearSaved() { _uiState.update { it.copy(isSaved = false) } }

    private fun applyTransform(transform: suspend (Bitmap) -> Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            transformMutex.withLock {
                val current = _uiState.value.currentBitmap ?: run {
                    _uiState.update { it.copy(isProcessing = false) }
                    return@withLock
                }
                runCatching { withContext(Dispatchers.Default) { transform(current) } }
                    .onSuccess { newBitmap ->
                        pushToUndoStack(current)
                        _uiState.update {
                            it.copy(currentBitmap = newBitmap, isProcessing = false, hasUnsavedChanges = true, canUndo = true)
                        }
                    }
                    .onFailure {
                        _uiState.update { it.copy(isProcessing = false, errorMessage = "Processing failed.") }
                    }
            }
        }
    }

    private fun pushToUndoStack(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, UNDO_JPEG_QUALITY, stream)
        undoStack.addLast(stream.toByteArray())
        if (undoStack.size > MAX_UNDO_STACK) {
            undoStack.removeFirst()
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.currentBitmap?.recycle()
        undoStack.clear()
    }
}
