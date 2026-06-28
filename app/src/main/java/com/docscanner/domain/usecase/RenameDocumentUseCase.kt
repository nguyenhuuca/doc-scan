package com.docscanner.domain.usecase

import com.docscanner.common.exceptions.DocumentNameException
import com.docscanner.data.repository.DocumentRepository

class RenameDocumentUseCase(private val repository: DocumentRepository) {

    companion object {
        private val FORBIDDEN_CHARS = Regex("""[/\\:*?"<>|]""")
    }

    suspend operator fun invoke(documentId: String, newName: String) {
        val trimmed = newName.trim()
        validate(trimmed)

        val current = repository.getDocumentById(documentId)
        if (current?.name == trimmed) return  // EC-7: skip write if unchanged

        repository.renameDocument(documentId, trimmed)
    }

    private fun validate(name: String) {
        when {
            name.isEmpty() -> throw DocumentNameException("Document name cannot be empty.")
            name.length > 50 -> throw DocumentNameException("Document name must be 50 characters or fewer.")
            FORBIDDEN_CHARS.containsMatchIn(name) -> throw DocumentNameException(
                "Document name contains invalid characters. Avoid: / \\ : * ? \" < > |"
            )
        }
    }
}
