package com.docscanner.ui.documentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.common.AppConfig
import com.docscanner.common.exceptions.DocumentNameException
import com.docscanner.domain.model.Document
import com.docscanner.domain.usecase.DeleteDocumentUseCase
import com.docscanner.domain.usecase.ExportPdfUseCase
import com.docscanner.R
import com.docscanner.domain.usecase.GetDocumentsUseCase
import com.docscanner.domain.usecase.RenameDocumentUseCase
import com.docscanner.ui.common.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class SortOrder { DATE_DESC, NAME_ASC }

private data class FilterState(
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC
)

data class DocumentListUiState(
    val documents: List<Document> = emptyList(),
    val totalDocumentCount: Int = 0,
    val isLoading: Boolean = true,
    val documentLimitReached: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val errorMessage: UiMessage? = null,
    val exportedFile: File? = null
)

class DocumentListViewModel(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val renameDocumentUseCase: RenameDocumentUseCase,
    private val exportPdfUseCase: ExportPdfUseCase
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<UiMessage?>(null)
    private val _exportedFile = MutableStateFlow<File?>(null)
    private val _filterState = MutableStateFlow(FilterState())

    val uiState: StateFlow<DocumentListUiState> = combine(
        getDocumentsUseCase(),
        _errorMessage,
        _exportedFile,
        _filterState
    ) { documents, error, exportedFile, filter ->
        DocumentListUiState(
            documents = filterAndSort(documents, filter.query, filter.sortOrder),
            totalDocumentCount = documents.size,
            isLoading = false,
            documentLimitReached = documents.size >= AppConfig.MAX_DOCUMENTS,
            searchQuery = filter.query,
            sortOrder = filter.sortOrder,
            errorMessage = error,
            exportedFile = exportedFile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DocumentListUiState()
    )

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun toggleSortOrder() {
        _filterState.update {
            it.copy(sortOrder = if (it.sortOrder == SortOrder.DATE_DESC) SortOrder.NAME_ASC else SortOrder.DATE_DESC)
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            runCatching { deleteDocumentUseCase(documentId) }
                .onFailure { _errorMessage.update { UiMessage(R.string.error_delete_document) } }
        }
    }

    fun renameDocument(documentId: String, newName: String) {
        viewModelScope.launch {
            runCatching { renameDocumentUseCase(documentId, newName) }
                .onFailure { e ->
                    _errorMessage.update {
                        when (e) {
                            is DocumentNameException -> UiMessage(R.string.error_invalid_document_name)
                            else -> UiMessage(R.string.error_rename_failed)
                        }
                    }
                }
        }
    }

    fun exportPdf(documentId: String) {
        viewModelScope.launch {
            runCatching { exportPdfUseCase(documentId) }
                .onSuccess { file -> _exportedFile.update { file } }
                .onFailure { _errorMessage.update { UiMessage(R.string.export_failed) } }
        }
    }

    fun clearError() { _errorMessage.update { null } }
    fun clearExportedFile() { _exportedFile.update { null } }
}

internal fun filterAndSort(documents: List<Document>, query: String, sortOrder: SortOrder): List<Document> {
    val filtered = if (query.isBlank()) documents
                   else documents.filter { it.name.contains(query, ignoreCase = true) }
    return when (sortOrder) {
        SortOrder.DATE_DESC -> filtered  // Room already returns DATE_DESC
        SortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
    }
}
