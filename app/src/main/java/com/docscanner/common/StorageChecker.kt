package com.docscanner.common

import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class StorageState {
    object Sufficient : StorageState()
    data class Warning(val availableBytes: Long) : StorageState()
    data class Blocked(val availableBytes: Long) : StorageState()
}

object StorageChecker {

    // Pure function — testable without Android (no StatFs)
    internal fun classify(availableBytes: Long): StorageState = when {
        availableBytes < AppConfig.MIN_STORAGE_BYTES -> StorageState.Blocked(availableBytes)
        availableBytes < AppConfig.MIN_STORAGE_WARNING_BYTES -> StorageState.Warning(availableBytes)
        else -> StorageState.Sufficient
    }

    // StatFs is a blocking statvfs() syscall — callers must dispatch to IO.
    fun availableBytes(directory: File): Long {
        val stat = StatFs(directory.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    suspend fun check(directory: File): StorageState = withContext(Dispatchers.IO) {
        classify(availableBytes(directory))
    }
}
