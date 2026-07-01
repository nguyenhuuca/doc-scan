package com.docscanner

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.docscanner.common.AppLanguage
import com.docscanner.di.AppContainer
import com.docscanner.ui.common.ExportCacheCleanup

class MyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(ExportCacheCleanup(cacheDir))
    }
}
