package com.curio.notes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.navigation.CurioNavHost
import com.curio.notes.ui.theme.CurioTheme
import com.curio.notes.ui.util.readStoredLanguage
import com.curio.notes.ui.util.wrapWithLanguage
import com.curio.notes.ui.util.writeStoredLanguage

class MainActivity : ComponentActivity() {
    // Applies the stored language before resources load, so the very first
    // frame (and every process restart) already uses the right locale.
    // Works on every API level, unlike AppCompatDelegate with ComponentActivity.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.wrapWithLanguage(readStoredLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CurioApplication).container
        setContent {
            val theme by container.settingsRepository.observeTheme()
                .collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
            val language by container.settingsRepository.observeLanguage()
                .collectAsStateWithLifecycle(initialValue = null)
            LaunchedEffect(language) {
                // Null is the "unknown yet" placeholder, not a real choice:
                // acting on it is what caused the recreate loop (it briefly
                // looked like SYSTEM while the stored value was still loading).
                val current = language ?: return@LaunchedEffect
                // Mirror matches DataStore in steady state, so this only fires
                // right after the user picks a new language: persist the sync
                // copy and recreate so the new locale takes effect at once.
                if (readStoredLanguage(applicationContext) != current) {
                    writeStoredLanguage(applicationContext, current)
                    recreate()
                }
            }
            CurioTheme(darkTheme = theme.isDark()) {
                CurioNavHost()
            }
        }
    }
}

@Composable
private fun AppTheme.isDark(): Boolean = when (this) {
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
    AppTheme.SYSTEM -> isSystemInDarkTheme()
}
