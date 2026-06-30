package com.docscanner.data.repository

import android.graphics.Bitmap
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    suspend fun getDocumentById(documentId: String): Document?
    suspend fun createDocument(name: String, firstPageBitmap: Bitmap): Document
    suspend fun addPage(documentId: String, bitmap: Bitmap): Page
    suspend fun addPages(documentId: String, bitmaps: List<Bitmap>): List<Page>
    suspend fun updatePage(documentId: String, page: Page, newBitmap: Bitmap): Page
    suspend fun reorderPages(documentId: String, reorderedPages: List<Page>)
    suspend fun deleteDocument(documentId: String)
    suspend fun deletePage(documentId: String, pageId: String)
    suspend fun renameDocument(documentId: String, newName: String)
    suspend fun getDocumentCount(): Int
    suspend fun getPageCount(documentId: String): Int
    suspend fun getPagesForDocument(documentId: String): List<Page>
    suspend fun exportPdf(documentId: String): File
    suspend fun pageFileExists(imagePath: String): Boolean
}
