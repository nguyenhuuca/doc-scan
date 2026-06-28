package com.docscanner.data

import com.docscanner.common.calcInSampleSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests the production calcInSampleSize() used by all BitmapFactory decode sites.
class BitmapUtilsTest {

    @Test
    fun `returns 1 when image fits within bounds`() {
        assertEquals(1, calcInSampleSize(100, 100, 200, 200))
    }

    @Test
    fun `returns 1 for exact match`() {
        assertEquals(1, calcInSampleSize(256, 256, 256, 256))
    }

    @Test
    fun `returns 2 for image twice the bound`() {
        assertEquals(2, calcInSampleSize(512, 512, 256, 256))
    }

    @Test
    fun `returns 4 for image four times the bound`() {
        assertEquals(4, calcInSampleSize(1024, 1024, 256, 256))
    }

    @Test
    fun `returns power-of-two sample size`() {
        val result = calcInSampleSize(1000, 1000, 256, 256)
        assertTrue("inSampleSize must be a power of two, got $result", result > 0 && (result and (result - 1)) == 0)
    }

    @Test
    fun `portrait image drives sample size when both dimensions exceed target`() {
        // 1024 × 4096 targeting 256 × 256 — both dimensions exceed target, height is 4x width
        val result = calcInSampleSize(1024, 4096, 256, 256)
        assertTrue("Expected sampleSize >= 4 for 4096px height, got $result", result >= 4)
    }

    @Test
    fun `zero dimensions return 1 without divide-by-zero`() {
        assertEquals(1, calcInSampleSize(0, 0, 256, 256))
        assertEquals(1, calcInSampleSize(0, 512, 256, 256))
    }
}
