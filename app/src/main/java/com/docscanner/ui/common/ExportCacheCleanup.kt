package com.docscanner.ui.common

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.File

class ExportCacheCleanup(private val cacheDir: File) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        val exportDir = File(cacheDir, "export")
        if (exportDir.exists()) {
            exportDir.listFiles()?.forEach { it.delete() }
        }
    }
}
