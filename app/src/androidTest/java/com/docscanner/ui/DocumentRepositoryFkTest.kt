package com.docscanner.ui

import android.graphics.Bitmap
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.docscanner.data.local.db.AppDatabase
import com.docscanner.data.local.filesystem.ImageStorage
import com.docscanner.data.local.filesystem.ThumbnailGenerator
import com.docscanner.data.pdf.PdfGenerator
import com.docscanner.data.repository.DocumentRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies that DocumentRepositoryImpl.createDocument() inserts the document
 * row BEFORE the page row, satisfying the FK constraint defined in PageEntity.
 * Regression test for the "Foreign key constraint failed code 787" bug.
 */
@RunWith(AndroidJUnit4::class)
class DocumentRepositoryFkTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DocumentRepositoryImpl
    private lateinit var tempDir: File

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tempDir = File(context.cacheDir, "fk_test_${System.currentTimeMillis()}").also { it.mkdirs() }

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            })
            .build()

        repository = DocumentRepositoryImpl(
            db.documentDao(),
            db.pageDao(),
            ImageStorage(tempDir),
            ThumbnailGenerator(tempDir),
            PdfGenerator(tempDir),
            db
        )
    }

    @After
    fun teardown() {
        db.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun createDocumentInsertsDocumentBeforePageNoFkViolation() = runBlocking {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val doc = repository.createDocument("Test Doc", bitmap)
        assertNotNull(doc)
        assertEquals("Test Doc", doc.name)
        assertEquals(1, doc.pageCount)
    }

    @Test
    fun pagesAreQueryableAfterCreateDocument() = runBlocking {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val doc = repository.createDocument("Doc 2", bitmap)
        val pages = repository.getPagesForDocument(doc.id)
        assertEquals(1, pages.size)
        assertEquals(1, pages.first().pageNumber)
    }

    @Test
    fun addPageLinksPageToExistingDocument() = runBlocking {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val doc = repository.createDocument("Doc 3", bitmap)
        repository.addPage(doc.id, bitmap)
        val pages = repository.getPagesForDocument(doc.id)
        assertEquals(2, pages.size)
    }
}
