package com.docscanner.domain.usecase

import com.docscanner.common.AppConfig
import com.docscanner.common.exceptions.DocumentNameException
import com.docscanner.data.repository.DocumentRepository

class RenameDocumentUseCase(private val repository: DocumentRepository) {

    suspend operator fun invoke(documentId: String, newName: String) {
        val trimmed = newName.trim()
        validate(trimmed)

        val current = repository.getDocumentById(documentId)
        if (current?.name == trimmed) return  // EC-7: skip write if unchanged

        repository.renameDocument(documentId, trimmed)
    }

    private fun validate(name: String) {
        when (DocumentNameValidator.validate(name)) {
            NameValidationError.EMPTY -> throw DocumentNameException("Document name cannot be empty.")
            NameValidationError.TOO_LONG -> throw DocumentNameException(
                "Document name must be ${AppConfig.MAX_DOCUMENT_NAME_LENGTH} characters or fewer."
            )
            NameValidationError.INVALID_CHARS -> throw DocumentNameException(
                "Document name contains invalid characters. Avoid: / \\ : * ? \" < > |"
            )
            null -> Unit
        }
    }
}
