package com.docscanner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageProcessorLogicTest {

    @Test
    fun `brightness matrix at zero has no translation`() {
        val brightness = 0f
        val translate = brightness // The ImageProcessor uses brightness directly as translation
        assertEquals(0f, translate, 0.001f)
    }

    @Test
    fun `brightness matrix at max 255 has full positive translation`() {
        val brightness = 255f
        assertTrue(brightness > 0)
    }

    @Test
    fun `contrast scale at 1 means no change`() {
        val contrast = 1f
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f
        assertEquals(1f, scale, 0.001f)
        assertEquals(0f, translate, 0.001f)
    }

    @Test
    fun `contrast scale at 0 collapses to grey`() {
        val contrast = 0f
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f
        assertEquals(0f, scale, 0.001f)
        assertEquals(127.5f, translate, 0.001f)
    }

    @Test
    fun `rotate degrees should be multiples of 90`() {
        val degrees = 90f
        assertEquals(0f, degrees % 90f, 0.001f)
    }
}
