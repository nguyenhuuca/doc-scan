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

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "docscanner.db").build()
    }

    val filesDir: File get() = appContext.filesDir
    val cacheDir: File get() = appContext.cacheDir

    val imageStorage: ImageStorage by lazy { ImageStorage(filesDir) }

    val thumbnailGenerator: ThumbnailGenerator by lazy { ThumbnailGenerator(filesDir) }

    val pdfGenerator: PdfGenerator by lazy { PdfGenerator(cacheDir) }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepositoryImpl(
            database.documentDao(),
            database.pageDao(),
            imageStorage,
            thumbnailGenerator,
            pdfGenerator,
            database
        )
    }

    val getDocumentsUseCase: GetDocumentsUseCase by lazy { GetDocumentsUseCase(documentRepository) }
    val saveDocumentUseCase: SaveDocumentUseCase by lazy { SaveDocumentUseCase(documentRepository, filesDir) }
    val exportPdfUseCase: ExportPdfUseCase by lazy { ExportPdfUseCase(documentRepository) }
    val deleteDocumentUseCase: DeleteDocumentUseCase by lazy { DeleteDocumentUseCase(documentRepository) }
    val renameDocumentUseCase: RenameDocumentUseCase by lazy { RenameDocumentUseCase(documentRepository) }
}
