package com.docscanner.di

import android.content.Context
import androidx.room.Room
import com.docscanner.data.local.db.AppDatabase
import com.docscanner.data.local.filesystem.ImageStorage
import com.docscanner.data.local.filesystem.ThumbnailGenerator
import com.docscanner.data.pdf.PdfGenerator
import com.docscanner.data.repository.DocumentRepository
import com.docscanner.data.repository.DocumentRepositoryImpl
import com.docscanner.domain.usecase.DeleteDocumentUseCase
import com.docscanner.domain.usecase.ExportPdfUseCase
import com.docscanner.domain.usecase.GetDocumentsUseCase
import com.docscanner.domain.usecase.RenameDocumentUseCase
import com.docscanner.domain.usecase.SaveDocumentUseCase
import java.io.File

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "docscanner.db"
        ).build()
    }

    val filesDir: File get() = context.filesDir
    val cacheDir: File get() = context.cacheDir

    val imageStorage: ImageStorage by lazy { ImageStorage(filesDir) }

    val thumbnailGenerator: ThumbnailGenerator by lazy { ThumbnailGenerator(filesDir) }

    val pdfGenerator: PdfGenerator by lazy { PdfGenerator(cacheDir) }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepositoryImpl(
            database.documentDao(),
            database.pageDao(),
            imageStorage,
            thumbnailGenerator,
            pdfGenerator
        )
    }

    val getDocumentsUseCase: GetDocumentsUseCase by lazy { GetDocumentsUseCase(documentRepository) }
    val saveDocumentUseCase: SaveDocumentUseCase by lazy { SaveDocumentUseCase(documentRepository, filesDir) }
    val exportPdfUseCase: ExportPdfUseCase by lazy { ExportPdfUseCase(documentRepository) }
    val deleteDocumentUseCase: DeleteDocumentUseCase by lazy { DeleteDocumentUseCase(documentRepository) }
    val renameDocumentUseCase: RenameDocumentUseCase by lazy { RenameDocumentUseCase(documentRepository) }

    init {
        cleanExportCache()
    }

    private fun cleanExportCache() {
        val exportDir = File(cacheDir, "export")
        if (exportDir.exists()) {
            val tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000L
            exportDir.listFiles()?.forEach { file ->
                if (file.lastModified() < tenMinutesAgo) {
                    file.delete()
                }
            }
        }
    }
}
