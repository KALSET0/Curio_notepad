package com.curio.notes.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SETTINGS_NAME = "curio_settings"
private val THEME_KEY = stringPreferencesKey("app_theme")
private val AI_PROVIDER_KEY = stringPreferencesKey("ai_provider")

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_NAME)

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    override fun observeTheme(): Flow<AppTheme> = dataStore.data.map { prefs ->
        runCatching {
            AppTheme.valueOf(prefs[THEME_KEY] ?: AppTheme.SYSTEM.name)
        }.getOrDefault(AppTheme.SYSTEM)
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { prefs -> prefs[THEME_KEY] = theme.name }
    }

    override fun observeAiProvider(): Flow<AiProviderChoice> = dataStore.data.map { prefs ->
        runCatching {
            AiProviderChoice.valueOf(prefs[AI_PROVIDER_KEY] ?: AiProviderChoice.MOCK.name)
        }.getOrDefault(AiProviderChoice.MOCK)
    }

    override suspend fun setAiProvider(choice: AiProviderChoice) {
        dataStore.edit { prefs -> prefs[AI_PROVIDER_KEY] = choice.name }
    }
}
