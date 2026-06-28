package com.docscanner.domain.usecase

import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Document
import kotlinx.coroutines.flow.Flow

class GetDocumentsUseCase(private val repository: DocumentRepository) {
    operator fun invoke(): Flow<List<Document>> = repository.getAllDocuments()
}
