package com.docscanner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests the scale-to-thumbnail logic mirroring ThumbnailGenerator.scaleThumbnail().
class ThumbnailGeneratorLogicTest {

    private val MAX_SIZE = 256

    @Test
    fun `square image at max size is not scaled`() {
        val (w, h) = scale(256, 256)
        assertEquals(256, w)
        assertEquals(256, h)
    }

    @Test
    fun `large square is scaled to 256 x 256`() {
        val (w, h) = scale(1024, 1024)
        assertEquals(256, w)
        assertEquals(256, h)
    }

    @Test
    fun `portrait image preserves aspect ratio`() {
        val (w, h) = scale(100, 200)
        assertTrue("Width $w should be <= 256", w <= MAX_SIZE)
        assertTrue("Height $h should be <= 256", h <= MAX_SIZE)
        assertEquals(1.0 / 2.0, w.toDouble() / h.toDouble(), 0.01)
    }

    @Test
    fun `landscape image preserves aspect ratio`() {
        val (w, h) = scale(400, 200)
        assertTrue(w <= MAX_SIZE)
        assertTrue(h <= MAX_SIZE)
        assertEquals(2.0, w.toDouble() / h.toDouble(), 0.01)
    }

    @Test
    fun `image smaller than 256 is not upscaled`() {
        val (w, h) = scale(100, 100)
        assertEquals(100, w)
        assertEquals(100, h)
    }

    @Test
    fun `zero-width input returns 0 x 0 without divide-by-zero`() {
        val (w, h) = scale(0, 0)
        assertEquals(0, w)
        assertEquals(0, h)
    }

    @Test
    fun `zero-height only input returns unchanged without crash`() {
        val (w, h) = scale(512, 0)
        assertEquals(512, w)
        assertEquals(0, h)
    }

    // Mirror of ThumbnailGenerator.scaleThumbnail logic
    private fun scale(inputWidth: Int, inputHeight: Int): Pair<Int, Int> {
        if (inputWidth == 0 || inputHeight == 0) return inputWidth to inputHeight
        if (inputWidth <= MAX_SIZE && inputHeight <= MAX_SIZE) return inputWidth to inputHeight
        val scaleFactor = MAX_SIZE.toFloat() / maxOf(inputWidth, inputHeight)
        return (inputWidth * scaleFactor).toInt() to (inputHeight * scaleFactor).toInt()
    }
}
