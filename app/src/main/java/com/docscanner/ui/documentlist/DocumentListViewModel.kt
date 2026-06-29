package com.docscanner.ui.documentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.common.AppConfig
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
        val MAX_DOCUMENTS = AppConfig.MAX_DOCUMENTS
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
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
            documentLimitReached = documents.size >= MAX_DOCUMENTS,
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

internal fun filterAndSort(documents: List<Document>, query: String, sortOrder: SortOrder): List<Document> {
    val filtered = if (query.isBlank()) documents
                   else documents.filter { it.name.contains(query, ignoreCase = true) }
    return when (sortOrder) {
        SortOrder.DATE_DESC -> filtered  // Room already returns DATE_DESC
        SortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
    }
}
