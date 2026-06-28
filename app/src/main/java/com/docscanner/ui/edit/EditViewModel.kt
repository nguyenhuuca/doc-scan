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
import kotlinx.coroutines.withContext

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
    }

    val documentId: String = savedStateHandle["documentId"] ?: ""
    val pageIndex: Int = savedStateHandle["pageIndex"] ?: 0

    private var currentPage: Page? = null
    private val undoStack = ArrayDeque<Bitmap>()

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

    fun rotate() {
        val bitmap = _uiState.value.currentBitmap ?: return
        applyTransform { ImageProcessor.rotateBitmap(bitmap) }
    }

    fun adjustBrightness(value: Float) {
        val bitmap = _uiState.value.currentBitmap ?: return
        applyTransform { ImageProcessor.adjustBrightness(bitmap, value) }
    }

    fun adjustContrast(value: Float) {
        val bitmap = _uiState.value.currentBitmap ?: return
        applyTransform { ImageProcessor.adjustContrast(bitmap, value) }
    }

    fun toGrayscale() {
        val bitmap = _uiState.value.currentBitmap ?: return
        applyTransform { ImageProcessor.toGrayscale(bitmap) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        _uiState.update {
            it.copy(
                currentBitmap = previous,
                canUndo = undoStack.isNotEmpty(),
                hasUnsavedChanges = undoStack.isNotEmpty()
            )
        }
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

    private fun applyTransform(transform: suspend () -> Bitmap) {
        val current = _uiState.value.currentBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            runCatching { transform() }
                .onSuccess { newBitmap ->
                    pushToUndoStack(current)
                    _uiState.update { it.copy(currentBitmap = newBitmap, isProcessing = false, hasUnsavedChanges = true, canUndo = true) }
                }
                .onFailure {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Processing failed.") }
                }
        }
    }

    private fun pushToUndoStack(bitmap: Bitmap) {
        undoStack.addLast(bitmap)
        if (undoStack.size > MAX_UNDO_STACK) {
            undoStack.removeFirst().recycle()
        }
    }

    override fun onCleared() {
        super.onCleared()
        undoStack.forEach { it.recycle() }
        undoStack.clear()
    }
}
