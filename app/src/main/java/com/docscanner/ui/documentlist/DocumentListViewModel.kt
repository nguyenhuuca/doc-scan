package com.docscanner.ui.documentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.common.exceptions.DocumentNameException
import com.docscanner.domain.model.Document
import com.docscanner.domain.usecase.DeleteDocumentUseCase
import com.docscanner.domain.usecase.ExportPdfUseCase
import com.docscanner.domain.usecase.GetDocumentsUseCase
import com.docscanner.domain.usecase.RenameDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DocumentListUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true,
    val documentLimitReached: Boolean = false,
    val errorMessage: String? = null,
    val exportedFile: File? = null
)

class DocumentListViewModel(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val renameDocumentUseCase: RenameDocumentUseCase,
    private val exportPdfUseCase: ExportPdfUseCase
) : ViewModel() {

    companion object {
        const val MAX_DOCUMENTS = 100
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _exportedFile = MutableStateFlow<File?>(null)

    val uiState: StateFlow<DocumentListUiState> = combine(
        getDocumentsUseCase(),
        _errorMessage,
        _exportedFile
    ) { documents, error, exportedFile ->
        DocumentListUiState(
            documents = documents,
            isLoading = false,
            documentLimitReached = documents.size >= MAX_DOCUMENTS,
            errorMessage = error,
            exportedFile = exportedFile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DocumentListUiState()
    )

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            runCatching { deleteDocumentUseCase(documentId) }
                .onFailure { _errorMessage.value = "Failed to delete document." }
        }
    }

    fun renameDocument(documentId: String, newName: String) {
        viewModelScope.launch {
            runCatching { renameDocumentUseCase(documentId, newName) }
                .onFailure { e ->
                    _errorMessage.value = when (e) {
                        is DocumentNameException -> e.message
                        else -> "Failed to rename document."
                    }
                }
        }
    }

    fun exportPdf(documentId: String) {
        viewModelScope.launch {
            runCatching { exportPdfUseCase(documentId) }
                .onSuccess { _exportedFile.value = it }
                .onFailure { _errorMessage.value = "Export failed. Please try again." }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearExportedFile() { _exportedFile.value = null }
}
