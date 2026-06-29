package com.docscanner.data

import com.docscanner.common.AppConfig
import com.docscanner.ui.common.StorageChecker
import com.docscanner.ui.common.StorageState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests StorageChecker.classify() — the pure classification function that
// contains all business logic. No Android APIs (no StatFs) needed.
class StorageCheckerLogicTest {

    @Test
    fun `exactly at WARNING_THRESHOLD returns Sufficient`() {
        val state = StorageChecker.classify(AppConfig.MIN_STORAGE_WARNING_BYTES)
        assertEquals(StorageState.Sufficient, state)
    }

    @Test
    fun `one byte below WARNING_THRESHOLD returns Warning`() {
        val state = StorageChecker.classify(AppConfig.MIN_STORAGE_WARNING_BYTES - 1)
        assertTrue(state is StorageState.Warning)
    }

    @Test
    fun `exactly at BLOCKING_THRESHOLD returns Warning`() {
        val state = StorageChecker.classify(AppConfig.MIN_STORAGE_BYTES)
        assertTrue(state is StorageState.Warning)
    }

    @Test
    fun `one byte below BLOCKING_THRESHOLD returns Blocked`() {
        val state = StorageChecker.classify(AppConfig.MIN_STORAGE_BYTES - 1)
        assertTrue(state is StorageState.Blocked)
    }

    @Test
    fun `zero bytes returns Blocked`() {
        val state = StorageChecker.classify(0L)
        assertTrue(state is StorageState.Blocked)
    }

    @Test
    fun `Warning carries available byte count`() {
        val available = AppConfig.MIN_STORAGE_BYTES
        val state = StorageChecker.classify(available) as StorageState.Warning
        assertEquals(available, state.availableBytes)
    }

    @Test
    fun `Blocked carries available byte count`() {
        val available = 1L
        val state = StorageChecker.classify(available) as StorageState.Blocked
        assertEquals(available, state.availableBytes)
    }
}
