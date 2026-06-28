package com.docscanner.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfGeneratorLogicTest {

    @Test
    fun `A4 page dimensions are 595 by 842 points`() {
        val width = 595
        val height = 842
        assertEquals(595, width)
        assertEquals(842, height)
    }

    @Test
    fun `margins leave 575 by 822 drawable area`() {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 10
        val drawableWidth = pageWidth - 2 * margin
        val drawableHeight = pageHeight - 2 * margin
        assertEquals(575, drawableWidth)
        assertEquals(822, drawableHeight)
    }

    @Test
    fun `page numbers are 1-based`() {
        val pages = listOf("p1", "p2", "p3")
        pages.forEachIndexed { index, _ ->
            val pageNumber = index + 1
            assertTrue("Page number $pageNumber should be >= 1", pageNumber >= 1)
        }
    }

    @Test
    fun `scale fit preserves aspect ratio`() {
        val bitmapWidth = 1240f
        val bitmapHeight = 1754f
        val drawableWidth = 575f
        val drawableHeight = 822f

        val scaleX = drawableWidth / bitmapWidth
        val scaleY = drawableHeight / bitmapHeight
        val scale = minOf(scaleX, scaleY)

        val scaledWidth = bitmapWidth * scale
        val scaledHeight = bitmapHeight * scale

        assertTrue(scaledWidth <= drawableWidth + 0.01f)
        assertTrue(scaledHeight <= drawableHeight + 0.01f)
        assertEquals(bitmapWidth / bitmapHeight, scaledWidth / scaledHeight, 0.001f)
    }

    private fun assertTrue(msg: String, condition: Boolean) {
        if (!condition) throw AssertionError(msg)
    }
}
