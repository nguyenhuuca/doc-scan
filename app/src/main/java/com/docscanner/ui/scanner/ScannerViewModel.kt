package com.docscanner.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.common.AppConfig
import com.docscanner.common.decodeSampledBitmap
import com.docscanner.common.exceptions.DocumentLimitException
import com.docscanner.common.exceptions.PageLimitException
import com.docscanner.common.exceptions.StorageFullException
import com.docscanner.R
import com.docscanner.domain.usecase.SaveDocumentUseCase
import com.docscanner.common.StorageChecker
import com.docscanner.common.StorageState
import com.docscanner.ui.common.UiMessage
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
    val errorMessage: UiMessage? = null,
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
        viewModelScope.launch {
            when (val state = StorageChecker.check(context.filesDir)) {
                is StorageState.Blocked -> {
                    _uiState.update {
                        it.copy(errorMessage = UiMessage(
                            R.string.low_storage_blocked_message,
                            listOf(AppConfig.MIN_STORAGE_BYTES / (1024 * 1024))
                        ))
                    }
                }
                is StorageState.Warning -> {
                    _uiState.update { it.copy(showStorageWarning = true, availableStorageBytes = state.availableBytes) }
                    launch()
                }
                is StorageState.Sufficient -> launch()
            }
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
                        decodeSampledBitmap(bytes, AppConfig.IMAGE_MAX_WIDTH, AppConfig.IMAGE_MAX_HEIGHT)
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
                _uiState.update { it.copy(isProcessing = false, errorMessage = mapSaveError(e)) }
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
                _uiState.update { it.copy(isProcessing = false, errorMessage = mapSaveError(e)) }
            }
        }
    }

    private fun mapSaveError(e: Throwable): UiMessage = when (e) {
        is PageLimitException -> UiMessage(R.string.error_page_limit_reached, listOf(AppConfig.MAX_PAGES))
        is DocumentLimitException -> UiMessage(R.string.error_document_limit_reached, listOf(AppConfig.MAX_DOCUMENTS))
        is StorageFullException -> UiMessage(R.string.error_storage_full_save)
        else -> UiMessage(R.string.error_save_failed)
    }

    fun onScanCancelled() {
        _uiState.update { it.copy(navigateBack = true) }
    }

    fun onScanError(message: UiMessage) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun clearStorageWarning() { _uiState.update { it.copy(showStorageWarning = false) } }
}
