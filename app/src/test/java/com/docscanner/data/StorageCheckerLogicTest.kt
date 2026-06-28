package com.docscanner.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageCheckerLogicTest {

    private val WARNING_THRESHOLD = 100L * 1024 * 1024   // 100 MB
    private val BLOCKING_THRESHOLD = 50L * 1024 * 1024   // 50 MB

    @Test
    fun `exactly 100 MB available returns Sufficient`() {
        val available = 100L * 1024 * 1024
        val state = classify(available)
        assertEquals("sufficient", state)
    }

    @Test
    fun `99 MB returns Warning`() {
        val available = 99L * 1024 * 1024
        val state = classify(available)
        assertEquals("warning", state)
    }

    @Test
    fun `exactly 50 MB returns Warning`() {
        val available = 50L * 1024 * 1024
        val state = classify(available)
        assertEquals("warning", state)
    }

    @Test
    fun `49 MB returns Blocked`() {
        val available = 49L * 1024 * 1024
        val state = classify(available)
        assertEquals("blocked", state)
    }

    @Test
    fun `zero bytes returns Blocked`() {
        val available = 0L
        val state = classify(available)
        assertEquals("blocked", state)
    }

    private fun classify(availableBytes: Long): String = when {
        availableBytes < BLOCKING_THRESHOLD -> "blocked"
        availableBytes < WARNING_THRESHOLD -> "warning"
        else -> "sufficient"
    }
}
