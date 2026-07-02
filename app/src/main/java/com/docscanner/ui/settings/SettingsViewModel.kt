package com.docscanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanner.data.local.filesystem.ImageStorage
import com.docscanner.data.repository.DocumentRepository
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
    val releaseNotes: List<ReleaseNote> = emptyList()
)

class SettingsViewModel(
    private val repository: DocumentRepository,
    private val imageStorage: ImageStorage,
    private val loadNotes: () -> List<ReleaseNote>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val count = repository.getDocumentCount()
            val storage = imageStorage.storageUsedBytes()
            val notes = withContext(Dispatchers.IO) { loadNotes() }
            _uiState.update {
                it.copy(documentCount = count, storageUsedBytes = storage, releaseNotes = notes)
            }
        }
    }
}
