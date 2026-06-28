package com.docscanner.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docscanner.ui.edit.ImageProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageProcessorTest {

    private fun solidBitmap(color: Int, size: Int = 4): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }

    @Test
    fun rotateBitmap90DegreesSwapsWidthAndHeight() = runBlocking {
        val src = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val result = ImageProcessor.rotateBitmap(src, 90f)
        assertEquals(50, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun rotateBitmap180DegreesPreservesDimensions() = runBlocking {
        val src = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        val result = ImageProcessor.rotateBitmap(src, 180f)
        assertEquals(80, result.width)
        assertEquals(60, result.height)
    }

    @Test
    fun adjustBrightnessAtZeroLeavesPixelsUnchanged() = runBlocking {
        val color = Color.rgb(100, 150, 200)
        val src = solidBitmap(color)
        val result = ImageProcessor.adjustBrightness(src, 0f)
        assertEquals(Color.red(color), Color.red(result.getPixel(0, 0)))
        assertEquals(Color.green(color), Color.green(result.getPixel(0, 0)))
        assertEquals(Color.blue(color), Color.blue(result.getPixel(0, 0)))
    }

    @Test
    fun adjustBrightnessAtMaxSaturatesPixelToWhite() = runBlocking {
        val src = solidBitmap(Color.rgb(100, 100, 100))
        val result = ImageProcessor.adjustBrightness(src, 255f)
        assertEquals(255, Color.red(result.getPixel(0, 0)))
        assertEquals(255, Color.green(result.getPixel(0, 0)))
        assertEquals(255, Color.blue(result.getPixel(0, 0)))
    }

    @Test
    fun adjustContrastAtOneLeavesPixelsUnchanged() = runBlocking {
        val color = Color.rgb(80, 120, 180)
        val src = solidBitmap(color)
        val result = ImageProcessor.adjustContrast(src, 1f)
        assertEqualsWithTolerance(Color.red(color), Color.red(result.getPixel(0, 0)), 2)
        assertEqualsWithTolerance(Color.green(color), Color.green(result.getPixel(0, 0)), 2)
        assertEqualsWithTolerance(Color.blue(color), Color.blue(result.getPixel(0, 0)), 2)
    }

    @Test
    fun adjustContrastAtZeroCollapsesAllPixelsToGrey() = runBlocking {
        val src = solidBitmap(Color.rgb(200, 50, 100))
        val result = ImageProcessor.adjustContrast(src, 0f)
        val r = Color.red(result.getPixel(0, 0))
        val g = Color.green(result.getPixel(0, 0))
        val b = Color.blue(result.getPixel(0, 0))
        // At scale=0, translate = 0.5 * 255 = 127.5 → all channels ~127-128
        assertTrue("Expected ~128 for R, got $r", r in 120..136)
        assertTrue("Expected ~128 for G, got $g", g in 120..136)
        assertTrue("Expected ~128 for B, got $b", b in 120..136)
    }

    @Test
    fun toGrayscaleProducesEqualRGBChannels() = runBlocking {
        val src = solidBitmap(Color.rgb(200, 100, 50))
        val result = ImageProcessor.toGrayscale(src)
        val px = result.getPixel(0, 0)
        assertEquals("R and G should match after grayscale", Color.red(px), Color.green(px))
        assertEquals("G and B should match after grayscale", Color.green(px), Color.blue(px))
    }

    @Test
    fun toGrayscaleOutputHasSameDimensionsAsInput() = runBlocking {
        val src = Bitmap.createBitmap(60, 40, Bitmap.Config.ARGB_8888)
        val result = ImageProcessor.toGrayscale(src)
        assertEquals(60, result.width)
        assertEquals(40, result.height)
    }

    private fun assertEqualsWithTolerance(expected: Int, actual: Int, tolerance: Int) {
        assertTrue(
            "Expected $expected ± $tolerance but was $actual",
            Math.abs(expected - actual) <= tolerance
        )
    }
}
