package com.docscanner.common

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Per-app language override, independent of the device's system language.
 *
 * API 33+: delegates to the platform [LocaleManager] — the OS persists the choice and
 * recreates activities automatically. [androidx.appcompat.app.AppCompatDelegate]'s equivalent
 * relies on a hidden-API context lookup that silently no-ops on apps that don't extend
 * AppCompatActivity (confirmed on a real device, API 35) — calling LocaleManager directly
 * avoids that failure mode and needs no extra dependency.
 *
 * API 26-32: no LocaleManager exists, so the tag is persisted in SharedPreferences and applied
 * by wrapping the base Context's Configuration in [wrap]. The caller must trigger
 * `Activity.recreate()` after [setTag] for the change to take visible effect on this range.
 */
object AppLanguage {
    private const val PREFS_NAME = "app_language_prefs"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    /** Currently selected language tag ("en", "vi"), or null for system default. */
    fun getTag(context: Context): String? {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (locales.isEmpty) null else locales[0]?.language
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANGUAGE_TAG, null)
    }

    /** Persists and applies [tag] (null = system default). */
    fun setTag(context: Context, tag: String?) {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            context.getSystemService(LocaleManager::class.java).applicationLocales = locales
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LANGUAGE_TAG, tag)
                .apply()
        }
    }

    /** Wraps [base] with the persisted locale override. No-op on API 33+ (OS handles it). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = getTag(base) ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }
}
