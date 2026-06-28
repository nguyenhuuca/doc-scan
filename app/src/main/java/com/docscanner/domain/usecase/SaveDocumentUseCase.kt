package com.docscanner.domain.usecase

import android.graphics.Bitmap
import android.os.StatFs
import com.docscanner.common.exceptions.DocumentLimitException
import com.docscanner.common.exceptions.PageLimitException
import com.docscanner.common.exceptions.StorageFullException
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveDocumentUseCase(
    private val repository: DocumentRepository,
    private val storageDir: File
) {
    companion object {
        private const val MIN_STORAGE_BYTES = 50L * 1024 * 1024
        private const val MAX_DOCUMENTS = 100
        private const val MAX_PAGES = 50
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    suspend fun createDocument(bitmap: Bitmap): Document {
        checkStorage()
        if (repository.getDocumentCount() >= MAX_DOCUMENTS) throw DocumentLimitException()
        val name = "Document ${DATE_FORMAT.format(Date())}"
        return repository.createDocument(name, bitmap)
    }

    suspend fun addPage(documentId: String, bitmap: Bitmap): Page {
        checkStorage()
        val pageCount = repository.getPageCount(documentId)
        if (pageCount >= MAX_PAGES) throw PageLimitException(documentId)
        return repository.addPage(documentId, bitmap)
    }

    private fun checkStorage() {
        val stat = StatFs(storageDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        if (availableBytes < MIN_STORAGE_BYTES) throw StorageFullException(availableBytes)
    }
}
