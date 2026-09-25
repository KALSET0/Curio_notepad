package com.curio.notes.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SettingsRepositoryTest {

    @Test
    fun `defaults are system theme and mock provider`() = runTest {
        val repository = SettingsRepositoryImpl(testStore())

        assertEquals(AppTheme.SYSTEM, repository.observeTheme().first())
        assertEquals(AiProviderChoice.MOCK, repository.observeAiProvider().first())
        assertEquals(AppLanguage.SYSTEM, repository.observeLanguage().first())
        assertEquals(false, repository.observeWebSearch().first())
        assertEquals("", repository.observeOllamaUrl().first())
        assertEquals("", repository.observeOllamaModel().first())
        assertEquals(false, repository.observeDeveloperMode().first())
    }

    @Test
    fun `choices persist`() = runTest {
        val repository = SettingsRepositoryImpl(testStore())

        repository.setTheme(AppTheme.DARK)
        repository.setAiProvider(AiProviderChoice.GEMINI)
        repository.setLanguage(AppLanguage.SPANISH)
        repository.setWebSearchEnabled(true)
        repository.setOllamaUrl("http://192.168.1.5:11434")
        repository.setOllamaModel("qwen3:8b")
        repository.setDeveloperMode(true)

        assertEquals(AppTheme.DARK, repository.observeTheme().first())
        assertEquals(AiProviderChoice.GEMINI, repository.observeAiProvider().first())
        assertEquals(AppLanguage.SPANISH, repository.observeLanguage().first())
        assertEquals(true, repository.observeWebSearch().first())
        assertEquals("http://192.168.1.5:11434", repository.observeOllamaUrl().first())
        assertEquals("qwen3:8b", repository.observeOllamaModel().first())
        assertEquals(true, repository.observeDeveloperMode().first())
    }

    @Test
    fun `unknown stored values fall back to defaults`() = runTest {
        val store = testStore()
        store.edit { prefs ->
            prefs[stringPreferencesKey("app_theme")] = "NOPE"
            prefs[stringPreferencesKey("ai_provider")] = "NOPE"
            prefs[stringPreferencesKey("app_language")] = "NOPE"
        }
        val repository = SettingsRepositoryImpl(store)

        assertEquals(AppTheme.SYSTEM, repository.observeTheme().first())
        assertEquals(AiProviderChoice.MOCK, repository.observeAiProvider().first())
        assertEquals(AppLanguage.SYSTEM, repository.observeLanguage().first())
    }

    private fun TestScope.testStore(): DataStore<Preferences> {
        val file = File.createTempFile("curio-test-settings", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        return PreferenceDataStoreFactory.create(scope = this, produceFile = { file })
    }
}
