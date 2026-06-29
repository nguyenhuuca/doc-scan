package com.docscanner.ui.common

import android.os.StatFs
import com.docscanner.common.AppConfig
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

    fun check(directory: File): StorageState {
        val stat = StatFs(directory.absolutePath)
        return classify(stat.availableBlocksLong * stat.blockSizeLong)
    }
}
