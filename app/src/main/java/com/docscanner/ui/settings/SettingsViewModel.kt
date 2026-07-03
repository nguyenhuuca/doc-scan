package com.docscanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.R
import com.docscanner.data.local.ReleaseNote
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.ui.common.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val documentCount: Int = 0,
    val storageUsedBytes: Long = 0L,
    val releaseNotes: List<ReleaseNote> = emptyList(),
    val errorMessage: UiMessage? = null
)

class SettingsViewModel(
    private val repository: DocumentRepository,
    // Suspend providers injected by the DI wiring so the ViewModel depends on
    // abstractions, not on the filesystem/assets layers directly.
    private val storageUsedBytes: suspend () -> Long,
    private val loadNotes: () -> List<ReleaseNote>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val count = repository.getDocumentCount()
                val storage = storageUsedBytes()
                val notes = withContext(Dispatchers.IO) { loadNotes() }
                Triple(count, storage, notes)
            }.onSuccess { (count, storage, notes) ->
                _uiState.update {
                    it.copy(documentCount = count, storageUsedBytes = storage, releaseNotes = notes)
                }
            }.onFailure {
                _uiState.update { it.copy(errorMessage = UiMessage(R.string.error_load_settings)) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
