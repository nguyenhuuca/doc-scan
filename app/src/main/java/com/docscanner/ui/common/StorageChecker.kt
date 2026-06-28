package com.docscanner.ui.common

import android.os.StatFs
import java.io.File

sealed class StorageState {
    object Sufficient : StorageState()
    data class Warning(val availableBytes: Long) : StorageState()
    data class Blocked(val availableBytes: Long) : StorageState()
}

object StorageChecker {
    internal const val WARNING_THRESHOLD = 100L * 1024 * 1024   // 100 MB
    internal const val BLOCKING_THRESHOLD = 50L * 1024 * 1024   // 50 MB

    // Pure function — testable without Android (no StatFs)
    internal fun classify(availableBytes: Long): StorageState = when {
        availableBytes < BLOCKING_THRESHOLD -> StorageState.Blocked(availableBytes)
        availableBytes < WARNING_THRESHOLD -> StorageState.Warning(availableBytes)
        else -> StorageState.Sufficient
    }

    fun check(directory: File): StorageState {
        val stat = StatFs(directory.absolutePath)
        return classify(stat.availableBlocksLong * stat.blockSizeLong)
    }
}
