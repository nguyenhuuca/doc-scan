package com.docscanner.data

import com.docscanner.data.pdf.PdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfGeneratorLogicTest {

    private val drawableW = PdfGenerator.DRAWABLE_W
    private val drawableH = PdfGenerator.DRAWABLE_H
    private val marginPt  = PdfGenerator.MARGIN_PT

    // ── Fit-to-page scaling ───────────────────────────────────────────────────

    @Test
    fun `landscape image is constrained by drawable width`() {
        val p = PdfGenerator.computePlacement(1000, 200)
        assertTrue("imgW should not exceed drawableW", p.imgW <= drawableW)
        assertTrue("imgH should not exceed drawableH", p.imgH <= drawableH)
    }

    @Test
    fun `portrait image is constrained by drawable height`() {
        val p = PdfGenerator.computePlacement(200, 1000)
        assertTrue("imgW should not exceed drawableW", p.imgW <= drawableW)
        assertTrue("imgH should not exceed drawableH", p.imgH <= drawableH)
    }

    @Test
    fun `square image fits within both dimensions`() {
        val p = PdfGenerator.computePlacement(500, 500)
        assertTrue("imgW should not exceed drawableW", p.imgW <= drawableW)
        assertTrue("imgH should not exceed drawableH", p.imgH <= drawableH)
    }

    @Test
    fun `image same size as drawable fills it exactly`() {
        val p = PdfGenerator.computePlacement(drawableW, drawableH)
        assertEquals("imgW should equal drawableW", drawableW, p.imgW)
        assertEquals("imgH should equal drawableH", drawableH, p.imgH)
    }

    // ── Centering ─────────────────────────────────────────────────────────────

    @Test
    fun `tall image is centered horizontally`() {
        val p = PdfGenerator.computePlacement(100, 1000)
        val expectedX = marginPt + (drawableW - p.imgW) / 2
        assertEquals("x should be centered", expectedX, p.x)
    }

    @Test
    fun `wide image is centered vertically`() {
        val p = PdfGenerator.computePlacement(1000, 100)
        val expectedY = marginPt + (drawableH - p.imgH) / 2
        assertEquals("y should be centered", expectedY, p.y)
    }

    @Test
    fun `x coordinate is never less than margin`() {
        val p = PdfGenerator.computePlacement(595, 842)
        assertTrue("x should be >= marginPt", p.x >= marginPt)
    }

    @Test
    fun `y coordinate is never less than margin`() {
        val p = PdfGenerator.computePlacement(595, 842)
        assertTrue("y should be >= marginPt", p.y >= marginPt)
    }

    // ── Aspect ratio ──────────────────────────────────────────────────────────

    @Test
    fun `aspect ratio is preserved after scaling`() {
        val pageW = 400
        val pageH = 600
        val p = PdfGenerator.computePlacement(pageW, pageH)
        val originalRatio = pageW.toDouble() / pageH
        val scaledRatio   = p.imgW.toDouble() / p.imgH
        assertEquals("aspect ratio should be preserved within 1%", originalRatio, scaledRatio, 0.01)
    }

    @Test
    fun `very wide image preserves aspect ratio within 2 percent`() {
        val pageW = 3000
        val pageH = 100
        val p = PdfGenerator.computePlacement(pageW, pageH)
        val originalRatio = pageW.toDouble() / pageH
        val scaledRatio   = p.imgW.toDouble() / p.imgH
        // Integer truncation introduces rounding error; allow 2% relative tolerance
        assertEquals("aspect ratio should be preserved", originalRatio, scaledRatio, originalRatio * 0.02)
    }
}
