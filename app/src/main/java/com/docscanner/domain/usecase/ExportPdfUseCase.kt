package com.docscanner.domain.usecase

import com.docscanner.common.exceptions.ExportException
import com.docscanner.data.repository.DocumentRepository
import java.io.File
import java.io.IOException

class ExportPdfUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(documentId: String): File {
        return try {
            repository.exportPdf(documentId)
        } catch (e: IOException) {
            throw ExportException(e)
        }
    }
}
