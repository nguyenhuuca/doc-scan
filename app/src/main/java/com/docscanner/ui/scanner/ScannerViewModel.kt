package com.docscanner.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.common.AppConfig
import com.docscanner.common.calcInSampleSize
import com.docscanner.common.exceptions.DocumentLimitException
import com.docscanner.common.exceptions.PageLimitException
import com.docscanner.common.exceptions.StorageFullException
import com.docscanner.domain.usecase.SaveDocumentUseCase
import com.docscanner.ui.common.StorageChecker
import com.docscanner.ui.common.StorageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannerUiState(
    val savedDocumentId: String? = null,
    val navigateBack: Boolean = false,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val showStorageWarning: Boolean = false,
    val availableStorageBytes: Long = 0L
)

class ScannerViewModel(
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val documentId: String = savedStateHandle["documentId"] ?: "new"

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun checkStorageAndLaunch(context: Context, launch: () -> Unit) {
        val state = StorageChecker.check(context.filesDir)
        when (state) {
            is StorageState.Blocked -> {
                _uiState.update { it.copy(errorMessage = "Not enough storage. Free up at least ${AppConfig.MIN_STORAGE_BYTES / (1024 * 1024)} MB to scan.") }
            }
            is StorageState.Warning -> {
                _uiState.update { it.copy(showStorageWarning = true, availableStorageBytes = state.availableBytes) }
                launch()
            }
            is StorageState.Sufficient -> launch()
        }
    }

    fun onScanSuccess(imageUris: List<Uri>, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            runCatching {
                val rawBytes = withContext(Dispatchers.IO) {
                    imageUris.mapNotNull { uri ->
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }.getOrNull()
                    }
                }
                if (rawBytes.isEmpty()) error("Could not read scanned images from scanner")

                val bitmaps = withContext(Dispatchers.Default) {
                    rawBytes.mapNotNull { bytes ->
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                        opts.inSampleSize = calcInSampleSize(opts.outWidth, opts.outHeight, AppConfig.IMAGE_MAX_WIDTH, AppConfig.IMAGE_MAX_HEIGHT)
                        opts.inJustDecodeBounds = false
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    }
                }
                if (bitmaps.isEmpty()) error("Failed to decode scanned images")

                if (documentId == "new") {
                    val doc = saveDocumentUseCase.createDocument(bitmaps.first())
                    if (bitmaps.size > 1) saveDocumentUseCase.addPages(doc.id, bitmaps.drop(1))
                    doc.id
                } else {
                    saveDocumentUseCase.addPages(documentId, bitmaps)
                    documentId
                }
            }.onSuccess { docId ->
                _uiState.update { it.copy(isProcessing = false, savedDocumentId = docId) }
            }.onFailure { e ->
                val message = when (e) {
                    is PageLimitException -> "Page limit (${AppConfig.MAX_PAGES}) reached."
                    is DocumentLimitException -> "Document limit (${AppConfig.MAX_DOCUMENTS}) reached."
                    is StorageFullException -> "Not enough storage to save."
                    else -> "Failed to save scan. Please try again."
                }
                _uiState.update { it.copy(isProcessing = false, errorMessage = message) }
            }
        }
    }

    fun onManualCropComplete(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            runCatching {
                if (documentId == "new") {
                    saveDocumentUseCase.createDocument(bitmap).id
                } else {
                    saveDocumentUseCase.addPage(documentId, bitmap)
                    documentId
                }
            }.onSuccess { docId ->
                _uiState.update { it.copy(isProcessing = false, savedDocumentId = docId) }
            }.onFailure { e ->
                val message = when (e) {
                    is PageLimitException -> "Page limit (${AppConfig.MAX_PAGES}) reached."
                    is StorageFullException -> "Not enough storage to save."
                    else -> "Failed to save. Please try again."
                }
                _uiState.update { it.copy(isProcessing = false, errorMessage = message) }
            }
        }
    }

    fun onScanCancelled() {
        _uiState.update { it.copy(navigateBack = true) }
    }

    fun onScanError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
