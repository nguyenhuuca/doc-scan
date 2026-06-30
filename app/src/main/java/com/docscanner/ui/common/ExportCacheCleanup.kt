package com.docscanner.ui.common

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.docscanner.common.AppConfig
import java.io.File

class ExportCacheCleanup(private val cacheDir: File) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        val exportDir = File(cacheDir, AppConfig.EXPORT_CACHE_DIR)
        if (exportDir.exists()) {
            exportDir.listFiles()?.forEach { it.delete() }
        }
    }
}
