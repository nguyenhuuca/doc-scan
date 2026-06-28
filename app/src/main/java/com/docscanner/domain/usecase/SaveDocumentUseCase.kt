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
    private val storageDir: File,
    // Injectable for testing — production default uses StatFs
    private val availableBytes: (File) -> Long = { dir ->
        val stat = StatFs(dir.absolutePath)
        stat.availableBlocksLong * stat.blockSizeLong
    }
) {
    companion object {
        internal const val MIN_STORAGE_BYTES = 50L * 1024 * 1024
        internal const val MAX_DOCUMENTS = 100
        internal const val MAX_PAGES = 50
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    suspend fun createDocument(bitmap: Bitmap): Document {
        validateStorage()
        validateDocumentCount(repository.getDocumentCount())
        val name = buildDocumentName()
        return repository.createDocument(name, bitmap)
    }

    suspend fun addPage(documentId: String, bitmap: Bitmap): Page {
        validateStorage()
        validatePageCount(repository.getPageCount(documentId), documentId)
        return repository.addPage(documentId, bitmap)
    }

    internal fun validateStorage() {
        val bytes = availableBytes(storageDir)
        if (bytes < MIN_STORAGE_BYTES) throw StorageFullException(bytes)
    }

    internal fun validateDocumentCount(count: Int) {
        if (count >= MAX_DOCUMENTS) throw DocumentLimitException()
    }

    internal fun validatePageCount(count: Int, documentId: String) {
        if (count >= MAX_PAGES) throw PageLimitException(documentId)
    }

    internal fun buildDocumentName(): String = "Document ${DATE_FORMAT.format(Date())}"
}
