package com.docscanner.domain.usecase

import com.docscanner.data.repository.DocumentRepository

class DeleteDocumentUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(documentId: String) {
        // Continues gracefully if files are already missing
        repository.deleteDocument(documentId)
    }
}
