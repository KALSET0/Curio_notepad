package com.curio.notes.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.curio.notes.domain.model.AiProviderChoice
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
    }

    @Test
    fun `choices persist`() = runTest {
        val repository = SettingsRepositoryImpl(testStore())

        repository.setTheme(AppTheme.DARK)
        repository.setAiProvider(AiProviderChoice.GEMINI)

        assertEquals(AppTheme.DARK, repository.observeTheme().first())
        assertEquals(AiProviderChoice.GEMINI, repository.observeAiProvider().first())
    }

    @Test
    fun `unknown stored values fall back to defaults`() = runTest {
        val store = testStore()
        store.edit { prefs ->
            prefs[stringPreferencesKey("app_theme")] = "NOPE"
            prefs[stringPreferencesKey("ai_provider")] = "NOPE"
        }
        val repository = SettingsRepositoryImpl(store)

        assertEquals(AppTheme.SYSTEM, repository.observeTheme().first())
        assertEquals(AiProviderChoice.MOCK, repository.observeAiProvider().first())
    }

    private fun TestScope.testStore(): DataStore<Preferences> {
        val file = File.createTempFile("curio-test-settings", ".preferences_pb").apply {
            delete()
            deleteOnExit()
        }
        return PreferenceDataStoreFactory.create(scope = this, produceFile = { file })
    }
}
