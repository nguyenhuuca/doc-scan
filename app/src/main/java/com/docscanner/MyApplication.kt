package com.docscanner

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.docscanner.di.AppContainer
import com.docscanner.ui.common.ExportCacheCleanup

class MyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(ExportCacheCleanup(cacheDir))
    }
}
