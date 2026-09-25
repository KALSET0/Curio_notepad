package com.curio.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.navigation.CurioNavHost
import com.curio.notes.ui.theme.CurioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CurioApplication).container
        setContent {
            val theme by container.settingsRepository.observeTheme()
                .collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
            val language by container.settingsRepository.observeLanguage()
                .collectAsStateWithLifecycle(initialValue = AppLanguage.SYSTEM)
            LaunchedEffect(language) {
                AppCompatDelegate.setApplicationLocales(language.toLocaleList())
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

private fun AppLanguage.toLocaleList(): LocaleListCompat = when (this) {
    AppLanguage.SYSTEM -> LocaleListCompat.forLanguageTags("")
    AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
    AppLanguage.SPANISH -> LocaleListCompat.forLanguageTags("es")
}
