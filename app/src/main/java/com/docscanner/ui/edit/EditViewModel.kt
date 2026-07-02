package com.docscanner.ui.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.common.AppConfig
import com.docscanner.common.calcInSampleSize
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
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
    val errorMessage: String? = null,
    val brightness: Float = 0f,
    val contrast: Float = 1f
)

@OptIn(FlowPreview::class)
class EditViewModel(
    private val repository: DocumentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val MAX_UNDO_STACK        = AppConfig.EDIT_MAX_UNDO_STACK
        private val UNDO_JPEG_QUALITY     = AppConfig.EDIT_UNDO_JPEG_QUALITY
        private val SLIDER_DEBOUNCE_MS    = AppConfig.EDIT_SLIDER_DEBOUNCE_MS
    }

    val documentId: String = savedStateHandle["documentId"] ?: ""
    val pageIndex: Int = savedStateHandle["pageIndex"] ?: 0

    private var currentPage: Page? = null
    // Bitmaps are stored as JPEG-compressed ByteArrays to limit memory.
    // 5 × ~200 KB compressed vs 5 × ~34 MB decoded — prevents OOM on high-res pages.
    private val undoStack = ArrayDeque<ByteArray>()
    private val transformMutex = Mutex()

    // Fixed reference bitmap that brightness/contrast are always recomputed from, so
    // slider values stay absolute instead of compounding on top of the last preview.
    private var baseBitmap: Bitmap? = null
    private var liveBrightness: Float = 0f
    private var liveContrast: Float = 1f
    // True once an undo entry has been pushed for the in-progress brightness/contrast
    // gesture, so a long slider drag doesn't flood the undo stack with one entry per tick.
    private var liveSessionActive: Boolean = false

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    private val previewTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        loadInitialPage()
        viewModelScope.launch {
            previewTrigger.debounce(SLIDER_DEBOUNCE_MS).collect {
                applyLiveAdjustment()
            }
        }
    }

    private fun recycleIfOrphaned(bitmap: Bitmap?, vararg keep: Bitmap?) {
        if (bitmap != null && keep.none { it === bitmap }) {
            bitmap.recycle()
        }
    }

    private fun loadInitialPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            runCatching {
                val pages = repository.getPagesForDocument(documentId)
                val page = pages.getOrNull(pageIndex) ?: error("Page $pageIndex not found")
                val bitmap = withContext(Dispatchers.Default) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(page.imagePath, opts)
                    if (opts.outWidth <= 0) null
                    else {
                        opts.inSampleSize = calcInSampleSize(
                            opts.outWidth, opts.outHeight,
                            AppConfig.IMAGE_MAX_WIDTH, AppConfig.IMAGE_MAX_HEIGHT
                        )
                        opts.inJustDecodeBounds = false
                        BitmapFactory.decodeFile(page.imagePath, opts)
                    }
                } ?: error("Failed to decode page image")
                page to bitmap
            }.onSuccess { (page, bitmap) ->
                currentPage = page
                baseBitmap = bitmap
                _uiState.update { it.copy(currentBitmap = bitmap, isProcessing = false) }
            }.onFailure {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Failed to load page.") }
            }
        }
    }

    fun rotate() = applyTransform { bitmap -> ImageProcessor.rotateBitmap(bitmap) }

    fun adjustBrightness(value: Float) {
        liveBrightness = value
        _uiState.update { it.copy(brightness = value) }
        previewTrigger.tryEmit(Unit)
    }

    fun adjustContrast(value: Float) {
        liveContrast = value
        _uiState.update { it.copy(contrast = value) }
        previewTrigger.tryEmit(Unit)
    }

    fun toGrayscale() = applyTransform { bitmap -> ImageProcessor.toGrayscale(bitmap) }

    private fun applyLiveAdjustment() {
        viewModelScope.launch {
            transformMutex.withLock {
                val base = baseBitmap ?: return@withLock
                _uiState.update { it.copy(isProcessing = true) }
                val firstInSession = !liveSessionActive
                runCatching {
                    withContext(Dispatchers.Default) {
                        val result = ImageProcessor.adjustBrightnessContrast(base, liveBrightness, liveContrast)
                        val undoBytes = if (firstInSession) {
                            val stream = ByteArrayOutputStream()
                            base.compress(Bitmap.CompressFormat.JPEG, UNDO_JPEG_QUALITY, stream)
                            stream.toByteArray()
                        } else null
                        result to undoBytes
                    }
                }.onSuccess { (result, undoBytes) ->
                    if (undoBytes != null) {
                        undoStack.addLast(undoBytes)
                        if (undoStack.size > MAX_UNDO_STACK) undoStack.removeFirst()
                        liveSessionActive = true
                    }
                    val old = _uiState.value.currentBitmap
                    _uiState.update {
                        it.copy(currentBitmap = result, isProcessing = false, hasUnsavedChanges = true, canUndo = true)
                    }
                    recycleIfOrphaned(old, result, base)
                }.onFailure {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Processing failed.") }
                }
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            val bytes = transformMutex.withLock {
                if (undoStack.isEmpty()) return@launch
                undoStack.removeLast()
            }
            val previous = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            val old = _uiState.value.currentBitmap
            val oldBase = baseBitmap
            baseBitmap = previous
            liveSessionActive = false
            liveBrightness = 0f
            liveContrast = 1f
            _uiState.update {
                it.copy(
                    currentBitmap = previous,
                    canUndo = undoStack.isNotEmpty(),
                    hasUnsavedChanges = undoStack.isNotEmpty(),
                    brightness = 0f,
                    contrast = 1f
                )
            }
            recycleIfOrphaned(old, previous)
            recycleIfOrphaned(oldBase, previous, old)
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
                baseBitmap = bitmap
                liveSessionActive = false
                liveBrightness = 0f
                liveContrast = 1f
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        hasUnsavedChanges = false,
                        isSaved = true,
                        canUndo = false,
                        brightness = 0f,
                        contrast = 1f
                    )
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
                val oldBase = baseBitmap
                runCatching {
                    withContext(Dispatchers.Default) {
                        val newBitmap = transform(current)
                        val stream = ByteArrayOutputStream()
                        current.compress(Bitmap.CompressFormat.JPEG, UNDO_JPEG_QUALITY, stream)
                        newBitmap to stream.toByteArray()
                    }
                }.onSuccess { (newBitmap, undoBytes) ->
                    undoStack.addLast(undoBytes)
                    if (undoStack.size > MAX_UNDO_STACK) undoStack.removeFirst()
                    baseBitmap = newBitmap
                    liveSessionActive = false
                    liveBrightness = 0f
                    liveContrast = 1f
                    _uiState.update {
                        it.copy(
                            currentBitmap = newBitmap,
                            isProcessing = false,
                            hasUnsavedChanges = true,
                            canUndo = true,
                            brightness = 0f,
                            contrast = 1f
                        )
                    }
                    recycleIfOrphaned(current, newBitmap)
                    recycleIfOrphaned(oldBase, newBitmap, current)
                }.onFailure {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Processing failed.") }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val current = _uiState.value.currentBitmap
        current?.recycle()
        recycleIfOrphaned(baseBitmap, current)
        undoStack.clear()
    }
}
