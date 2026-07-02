package com.docscanner.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.docscanner.domain.model.Document
import com.docscanner.domain.model.Page
import com.docscanner.ui.edit.EditViewModel
import com.docscanner.ui.edit.ImageProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Regression tests for the compounding brightness/contrast bug: slider adjustments
 * must always be recomputed from the fixed base bitmap, never stacked on top of the
 * previous preview. A regression makes repeated slider events saturate the image
 * to pure white/black instead of tracking the absolute slider value.
 */
class EditViewModelRegressionTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var imageFile: File
    private lateinit var testPage: Page

    @Before
    fun createTestImage() {
        // Solid mid-gray so brightness/contrast effects are measurable and JPEG-stable
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(128, 128, 128))
        imageFile = File(context.cacheDir, "edit_regression_${System.currentTimeMillis()}.jpg")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        bitmap.recycle()
        testPage = Page("p1", "doc1", 1, imageFile.absolutePath, 0L)
    }

    @After
    fun deleteTestImage() {
        imageFile.delete()
    }

    private fun makeRepo() = object : com.docscanner.data.repository.DocumentRepository {
        override fun getAllDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: String): Document? = null
        override suspend fun createDocument(n: String, b: Bitmap): Document = Document("d", n, 0L, 0L, 1, null)
        override suspend fun addPage(id: String, b: Bitmap): Page = Page("p", id, 1, "/p.jpg", 0L)
        override suspend fun addPages(id: String, bitmaps: List<Bitmap>): List<Page> =
            bitmaps.mapIndexed { i, _ -> Page("p$i", id, i + 1, "/p$i.jpg", 0L) }
        override suspend fun updatePage(id: String, p: Page, b: Bitmap): Page = p
        override suspend fun reorderPages(id: String, pages: List<Page>) {}
        override suspend fun deleteDocument(id: String) {}
        override suspend fun deletePage(id: String, pageId: String) {}
        override suspend fun renameDocument(id: String, name: String) {}
        override suspend fun getDocumentCount(): Int = 0
        override suspend fun getPageCount(id: String): Int = 1
        override suspend fun getPagesForDocument(id: String): List<Page> = listOf(testPage)
        override suspend fun exportPdf(id: String): File = File("/f.pdf")
        override suspend fun pageFileExists(path: String): Boolean = true
    }

    private fun makeViewModel(): EditViewModel {
        val savedState = SavedStateHandle(mapOf("documentId" to "doc1", "pageIndex" to 0))
        return EditViewModel(makeRepo(), savedState)
    }

    private suspend fun awaitLoadedBitmap(vm: EditViewModel): Bitmap =
        withTimeout(5_000) {
            vm.uiState.first { it.currentBitmap != null && !it.isProcessing }.currentBitmap!!
        }

    private suspend fun awaitBitmapChange(vm: EditViewModel, previous: Bitmap): Bitmap =
        withTimeout(5_000) {
            vm.uiState.first {
                it.currentBitmap != null && it.currentBitmap !== previous && !it.isProcessing
            }.currentBitmap!!
        }

    private fun assertPixelsClose(expected: Int, actual: Int, tolerance: Int, message: String) {
        val dr = abs(Color.red(expected) - Color.red(actual))
        val dg = abs(Color.green(expected) - Color.green(actual))
        val db = abs(Color.blue(expected) - Color.blue(actual))
        assertTrue(
            "$message: expected ~(${Color.red(expected)},${Color.green(expected)},${Color.blue(expected)}) " +
                "but was (${Color.red(actual)},${Color.green(actual)},${Color.blue(actual)})",
            dr <= tolerance && dg <= tolerance && db <= tolerance
        )
    }

    @Test
    fun successiveBrightnessEventsDoNotCompound() = runBlocking {
        val vm = makeViewModel()
        val base = awaitLoadedBitmap(vm)

        // Simulate a slider drag firing the debounce twice: 50 then 100.
        vm.adjustBrightness(50f)
        val first = awaitBitmapChange(vm, base)
        vm.adjustBrightness(100f)
        val second = awaitBitmapChange(vm, first)

        // Expected: brightness 100 applied once to the ORIGINAL image. A compounding
        // regression would apply +100 on top of the already +50 preview (~250 vs ~228).
        val original = BitmapFactory.decodeFile(imageFile.absolutePath)
        val expected = ImageProcessor.adjustBrightnessContrast(original, 100f, 1f)

        assertPixelsClose(
            expected.getPixel(32, 32), second.getPixel(32, 32), 4,
            "Second brightness event must reflect only the final slider value"
        )
    }

    @Test
    fun brightnessThenContrastMatchesSingleCombinedPass() = runBlocking {
        val vm = makeViewModel()
        val base = awaitLoadedBitmap(vm)

        vm.adjustBrightness(60f)
        val first = awaitBitmapChange(vm, base)
        vm.adjustContrast(1.5f)
        val second = awaitBitmapChange(vm, first)

        // Both values must be applied together from the base, not contrast on top of
        // an already-brightened bitmap.
        val original = BitmapFactory.decodeFile(imageFile.absolutePath)
        val expected = ImageProcessor.adjustBrightnessContrast(original, 60f, 1.5f)

        assertPixelsClose(
            expected.getPixel(32, 32), second.getPixel(32, 32), 4,
            "Contrast after brightness must equal one combined pass from the base image"
        )
    }

    @Test
    fun undoRestoresBaseImageAndResetsSliderValues() = runBlocking {
        val vm = makeViewModel()
        val base = awaitLoadedBitmap(vm)
        val basePixel = base.getPixel(32, 32)

        vm.adjustBrightness(120f)
        val adjusted = awaitBitmapChange(vm, base)

        vm.undo()
        val restored = awaitBitmapChange(vm, adjusted)
        val state = vm.uiState.value

        assertEquals("Undo must reset brightness slider", 0f, state.brightness, 0f)
        assertEquals("Undo must reset contrast slider", 1f, state.contrast, 0f)
        assertFalse("Undo stack must be empty after undoing the only change", state.canUndo)
        // Undo snapshots are stored as JPEG (quality 75), so allow re-encode tolerance.
        assertPixelsClose(
            basePixel, restored.getPixel(32, 32), 8,
            "Undo must restore the pre-adjustment image"
        )
    }

    @Test
    fun combinedAdjustAtNeutralValuesIsIdentity() = runBlocking {
        val original = BitmapFactory.decodeFile(imageFile.absolutePath)
        val result = ImageProcessor.adjustBrightnessContrast(original, 0f, 1f)
        assertEquals(
            "brightness=0 contrast=1 must leave pixels unchanged",
            original.getPixel(32, 32), result.getPixel(32, 32)
        )
    }
}
