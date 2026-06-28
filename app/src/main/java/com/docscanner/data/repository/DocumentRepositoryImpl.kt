package com.docscanner.data.repository

import android.graphics.Bitmap
import com.docscanner.data.local.db.DocumentDao
import com.docscanner.data.local.db.PageDao
import com.docscanner.data.local.entity.DocumentEntity
import com.docscanner.data.local.entity.PageEntity
import com.docscanner.data.local.filesystem.ImageStorage
import com.docscanner.data.local.filesystem.ThumbnailGenerator
import com.docscanner.data.pdf.PdfGenerator
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    private val imageStorage: ImageStorage,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val pdfGenerator: PdfGenerator
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> =
        documentDao.getAllDocuments().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getDocumentById(documentId: String): Document? =
        documentDao.getDocumentById(documentId)?.toDomain()

    override suspend fun createDocument(name: String, firstPageBitmap: Bitmap): Document {
        val documentId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val imagePath = imageStorage.savePageImage(documentId, 1, firstPageBitmap, now)
        val thumbnailPath = thumbnailGenerator.generateThumbnail(documentId, firstPageBitmap)

        // Insert document FIRST — page has a FK to documents.id
        val docEntity = DocumentEntity(
            id = documentId,
            name = name,
            createdAt = now,
            updatedAt = now,
            pageCount = 1,
            thumbnailPath = thumbnailPath
        )
        documentDao.insertDocument(docEntity)

        val pageEntity = PageEntity(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            pageNumber = 1,
            imagePath = imagePath,
            createdAt = now
        )
        pageDao.insertPage(pageEntity)

        return docEntity.toDomain()
    }

    override suspend fun addPage(documentId: String, bitmap: Bitmap): Page {
        val now = System.currentTimeMillis()
        val currentPages = pageDao.getPagesByDocumentIdSync(documentId)
        val nextPageNumber = (currentPages.maxOfOrNull { it.pageNumber } ?: 0) + 1

        val imagePath = imageStorage.savePageImage(documentId, nextPageNumber, bitmap, now)
        val pageId = UUID.randomUUID().toString()
        val pageEntity = PageEntity(
            id = pageId,
            documentId = documentId,
            pageNumber = nextPageNumber,
            imagePath = imagePath,
            createdAt = now
        )
        pageDao.insertPage(pageEntity)

        val newPageCount = currentPages.size + 1
        val doc = documentDao.getDocumentById(documentId)
        documentDao.updateDocumentMeta(documentId, newPageCount, now, doc?.thumbnailPath)

        return pageEntity.toDomain()
    }

    override suspend fun updatePage(documentId: String, page: Page, newBitmap: Bitmap): Page {
        val now = System.currentTimeMillis()
        val newImagePath = imageStorage.updatePageImage(documentId, page.pageNumber, newBitmap, page.imagePath, now)
        val updatedEntity = PageEntity(
            id = page.id,
            documentId = documentId,
            pageNumber = page.pageNumber,
            imagePath = newImagePath,
            createdAt = page.createdAt
        )
        pageDao.updatePage(updatedEntity)

        if (page.pageNumber == 1) {
            val thumbnailPath = thumbnailGenerator.generateThumbnail(documentId, newBitmap)
            val pageCount = pageDao.getPageCount(documentId)
            documentDao.updateDocumentMeta(documentId, pageCount, now, thumbnailPath)
        } else {
            val pageCount = pageDao.getPageCount(documentId)
            val doc = documentDao.getDocumentById(documentId)
            documentDao.updateDocumentMeta(documentId, pageCount, now, doc?.thumbnailPath)
        }

        return updatedEntity.toDomain()
    }

    override suspend fun reorderPages(documentId: String, reorderedPages: List<Page>) {
        val now = System.currentTimeMillis()
        reorderedPages.forEachIndexed { index, page ->
            pageDao.updatePageNumber(page.id, index + 1)
        }
        val newFirstPage = reorderedPages.firstOrNull()
        val thumbnailPath = if (newFirstPage != null) {
            thumbnailGenerator.generateThumbnailFromPath(documentId, newFirstPage.imagePath)
        } else null
        val doc = documentDao.getDocumentById(documentId)
        documentDao.updateDocumentMeta(documentId, reorderedPages.size, now, thumbnailPath ?: doc?.thumbnailPath)
    }

    override suspend fun deleteDocument(documentId: String) {
        imageStorage.deleteDocumentImages(documentId)
        documentDao.deleteDocument(documentId)
    }

    override suspend fun deletePage(documentId: String, pageId: String) {
        val pages = pageDao.getPagesByDocumentIdSync(documentId)
        val pageToDelete = pages.find { it.id == pageId } ?: return
        imageStorage.deletePageImage(pageToDelete.imagePath)
        pageDao.deletePage(pageId)

        val remainingPages = pages.filter { it.id != pageId }
            .sortedBy { it.pageNumber }
        remainingPages.forEachIndexed { index, page ->
            if (page.pageNumber != index + 1) {
                pageDao.updatePageNumber(page.id, index + 1)
            }
        }

        val now = System.currentTimeMillis()
        val newFirstPage = remainingPages.firstOrNull()
        val thumbnailPath = if (newFirstPage != null && pageToDelete.pageNumber == 1) {
            thumbnailGenerator.generateThumbnailFromPath(documentId, newFirstPage.imagePath)
        } else {
            documentDao.getDocumentById(documentId)?.thumbnailPath
        }
        documentDao.updateDocumentMeta(documentId, remainingPages.size, now, thumbnailPath)
    }

    override suspend fun renameDocument(documentId: String, newName: String) {
        documentDao.renameDocument(documentId, newName, System.currentTimeMillis())
    }

    override suspend fun getDocumentCount(): Int = documentDao.getDocumentCount()

    override suspend fun getPageCount(documentId: String): Int = pageDao.getPageCount(documentId)

    override suspend fun getPagesForDocument(documentId: String): List<Page> =
        pageDao.getPagesByDocumentIdSync(documentId).map { it.toDomain() }

    override suspend fun exportPdf(documentId: String): File {
        val pages = pageDao.getPagesByDocumentIdSync(documentId).sortedBy { it.pageNumber }
        return pdfGenerator.generatePdf(documentId, pages.map { it.imagePath })
    }

    override fun pageFileExists(imagePath: String): Boolean =
        imageStorage.pageFileExists(imagePath)

    private fun DocumentEntity.toDomain() = Document(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pageCount = pageCount,
        thumbnailPath = thumbnailPath
    )

    private fun PageEntity.toDomain() = Page(
        id = id,
        documentId = documentId,
        pageNumber = pageNumber,
        imagePath = imagePath,
        createdAt = createdAt
    )
}
