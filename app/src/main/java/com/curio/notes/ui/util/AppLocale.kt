package com.curio.notes.ui.util

import android.content.Context
import android.content.res.Configuration
import com.curio.notes.domain.model.AppLanguage
import java.util.Locale

private const val LANGUAGE_SYNC_PREFS = "curio_language_sync"
private const val LANGUAGE_SYNC_KEY = "app_language"

/**
 * Synchronous boot cache of the language choice. DataStore stays the source
 * of truth; this mirror exists because [android.app.Activity.attachBaseContext]
 * must read the language synchronously before any coroutine can run.
 * Written on every observed change, so it never drifts from DataStore.
 */
fun readStoredLanguage(context: Context): AppLanguage {
    val name = context
        .getSharedPreferences(LANGUAGE_SYNC_PREFS, Context.MODE_PRIVATE)
        .getString(LANGUAGE_SYNC_KEY, null)
    return runCatching {
        AppLanguage.valueOf(name ?: AppLanguage.SYSTEM.name)
    }.getOrDefault(AppLanguage.SYSTEM)
}

fun writeStoredLanguage(context: Context, language: AppLanguage) {
    context
        .getSharedPreferences(LANGUAGE_SYNC_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(LANGUAGE_SYNC_KEY, language.name)
        .apply()
}

fun AppLanguage.toLocale(): Locale? = when (this) {
    AppLanguage.SYSTEM -> null
    AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
    AppLanguage.SPANISH -> Locale.forLanguageTag("es")
}

/** Returns a context whose resources resolve in the chosen language.
 * SYSTEM keeps the device configuration untouched. */
fun Context.wrapWithLanguage(language: AppLanguage): Context {
    val locale = language.toLocale() ?: return this
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
