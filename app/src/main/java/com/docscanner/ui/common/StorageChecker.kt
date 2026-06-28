package com.docscanner.ui.common

import android.os.StatFs
import java.io.File

sealed class StorageState {
    object Sufficient : StorageState()
    data class Warning(val availableBytes: Long) : StorageState()
    data class Blocked(val availableBytes: Long) : StorageState()
}

object StorageChecker {
    private const val WARNING_THRESHOLD = 100L * 1024 * 1024   // 100 MB
    private const val BLOCKING_THRESHOLD = 50L * 1024 * 1024   // 50 MB

    fun check(directory: File): StorageState {
        val stat = StatFs(directory.absolutePath)
        val available = stat.availableBlocksLong * stat.blockSizeLong
        return when {
            available < BLOCKING_THRESHOLD -> StorageState.Blocked(available)
            available < WARNING_THRESHOLD -> StorageState.Warning(available)
            else -> StorageState.Sufficient
        }
    }
}
