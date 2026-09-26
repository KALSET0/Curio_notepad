package com.curio.notes.ai

import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenerationTrackerTest {

    @Test
    fun `labels resolve per provider`() = runTest {
        val settings = FakeSettings()
        // Unconfined scope: Eagerly-shared flows propagate synchronously.
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        try {
            val tracker = GenerationTracker(settings, scope, clock = { 0L })

            assertEquals("Mock AI", tracker.currentLabel.value)

            settings.provider.value = AiProviderChoice.GEMINI
            assertEquals("Gemini · gemini-3.8-flash", tracker.currentLabel.value)

            settings.provider.value = AiProviderChoice.OLLAMA
            settings.ollamaModel.value = "qwen3:8b"
            assertEquals("Ollama · qwen3:8b", tracker.currentLabel.value)

            settings.ollamaModel.value = ""
            assertEquals(
                "Ollama · ${com.curio.notes.ai.providers.OLLAMA_DEFAULT_MODEL}",
                tracker.currentLabel.value
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `track returns value and stores stats`() = runTest {
        var now = 0L
        val tracker = GenerationTracker(FakeSettings(), backgroundScope, clock = { now })

        val result = tracker.track(7L) {
            now += 8_400L
            "answer"
        }

        assertEquals("answer", result)
        assertEquals(GenerationInfo("Mock AI", 8_400L), tracker.statsFor(7L))
        assertNull(tracker.statsFor(8L))
    }

    @Test
    fun `track records even on failure`() = runTest {
        var now = 0L
        val tracker = GenerationTracker(FakeSettings(), backgroundScope, clock = { now })

        try {
            tracker.track(7L) {
                now += 500L
                throw IllegalStateException("boom")
            }
        } catch (e: IllegalStateException) {
            // expected
        }

        assertEquals(GenerationInfo("Mock AI", 500L), tracker.statsFor(7L))
    }

    @Test
    fun `durations format with one decimal`() {
        assertEquals("0.0 s", formatGenerationDuration(0L))
        assertEquals("1.0 s", formatGenerationDuration(1_000L))
        assertEquals("8.4 s", formatGenerationDuration(8_400L))
        assertEquals("12.5 s", formatGenerationDuration(12_500L))
    }

    private class FakeSettings : SettingsRepository {
        val theme = MutableStateFlow(AppTheme.SYSTEM)
        val provider = MutableStateFlow(AiProviderChoice.MOCK)
        val language = MutableStateFlow(AppLanguage.SYSTEM)
        val webSearch = MutableStateFlow(false)
        val ollamaUrl = MutableStateFlow("")
        val ollamaModel = MutableStateFlow("")
        val developerMode = MutableStateFlow(false)

        override fun observeTheme(): Flow<AppTheme> = theme
        override suspend fun setTheme(theme: AppTheme) {
            this.theme.value = theme
        }

        override fun observeAiProvider(): Flow<AiProviderChoice> = provider
        override suspend fun setAiProvider(choice: AiProviderChoice) {
            provider.value = choice
        }

        override fun observeLanguage(): Flow<AppLanguage> = language
        override suspend fun setLanguage(language: AppLanguage) {
            this.language.value = language
        }

        override fun observeWebSearch(): Flow<Boolean> = webSearch
        override suspend fun setWebSearchEnabled(enabled: Boolean) {
            webSearch.value = enabled
        }

        override fun observeOllamaUrl(): Flow<String> = ollamaUrl
        override suspend fun setOllamaUrl(url: String) {
            ollamaUrl.value = url
        }

        override fun observeOllamaModel(): Flow<String> = ollamaModel
        override suspend fun setOllamaModel(model: String) {
            ollamaModel.value = model
        }

        override fun observeDeveloperMode(): Flow<Boolean> = developerMode
        override suspend fun setDeveloperMode(enabled: Boolean) {
            developerMode.value = enabled
        }

        override fun observeSetupComplete(): Flow<Boolean> = flowOf(false)
        override suspend fun setSetupComplete(completed: Boolean) = Unit
    }
}
