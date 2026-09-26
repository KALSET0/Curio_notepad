package com.curio.notes.ui.settings

import com.curio.notes.ai.ApiKeyCheck
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.ApiKeyKind
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `five quick taps unlock developer mode`() = runTest {
        val settings = FakeSettings()
        var now = 0L
        val viewModel = viewModel(settings, clock = { now })
        backgroundScope.launch { viewModel.developerMode.collect {} }
        advanceUntilIdle()

        repeat(5) { viewModel.onVersionTap() }
        advanceUntilIdle()

        assertEquals(true, settings.developerMode.value)
    }

    @Test
    fun `slow taps do not unlock`() = runTest {
        val settings = FakeSettings()
        var now = 0L
        val viewModel = viewModel(settings, clock = { now })
        backgroundScope.launch { viewModel.developerMode.collect {} }
        advanceUntilIdle()

        repeat(5) {
            viewModel.onVersionTap()
            now += 3_000L
        }
        advanceUntilIdle()

        assertEquals(false, settings.developerMode.value)
    }

    @Test
    fun `disabling with ollama active falls back to mock`() = runTest {
        val settings = FakeSettings().apply {
            provider.value = AiProviderChoice.OLLAMA
            developerMode.value = true
        }
        val viewModel = viewModel(settings, clock = { 0L })
        backgroundScope.launch { viewModel.aiProvider.collect {} }
        backgroundScope.launch { viewModel.developerMode.collect {} }
        advanceUntilIdle()

        viewModel.setDeveloperMode(false)
        advanceUntilIdle()

        assertEquals(false, settings.developerMode.value)
        assertEquals(AiProviderChoice.MOCK, settings.provider.value)
    }

    @Test
    fun `disabling with mock active keeps provider`() = runTest {
        val settings = FakeSettings().apply { developerMode.value = true }
        val viewModel = viewModel(settings, clock = { 0L })
        backgroundScope.launch { viewModel.aiProvider.collect {} }
        backgroundScope.launch { viewModel.developerMode.collect {} }
        advanceUntilIdle()

        viewModel.setDeveloperMode(false)
        advanceUntilIdle()

        assertEquals(AiProviderChoice.MOCK, settings.provider.value)
    }

    @Test
    fun `test connection reports ok and failure`() = runTest {
        val okViewModel = viewModel(FakeSettings(), clock = { 0L }, testOllama = { true })
        backgroundScope.launch { okViewModel.ollamaTest.collect {} }
        advanceUntilIdle()

        okViewModel.testOllama()
        advanceUntilIdle()

        assertEquals(OllamaTestState.Ok, okViewModel.ollamaTest.value)

        val badViewModel = viewModel(FakeSettings(), clock = { 0L }, testOllama = { false })
        backgroundScope.launch { badViewModel.ollamaTest.collect {} }
        advanceUntilIdle()

        badViewModel.testOllama()
        advanceUntilIdle()

        assertEquals(OllamaTestState.Failed, badViewModel.ollamaTest.value)
    }

    @Test
    fun `saveApiKey stores and clearApiKey removes`() = runTest {
        val viewModel = viewModel(FakeSettings(), clock = { 0L })
        backgroundScope.launch { viewModel.geminiKey.collect {} }
        advanceUntilIdle()

        viewModel.saveApiKey(ApiKeyKind.GEMINI, "secret")
        advanceUntilIdle()
        assertEquals("secret", viewModel.geminiKey.value)

        viewModel.clearApiKey(ApiKeyKind.GEMINI)
        advanceUntilIdle()
        assertEquals("", viewModel.geminiKey.value)
    }

    @Test
    fun `testGeminiKey maps validation outcomes`() = runTest {
        val cases = listOf(
            ApiKeyCheck.VALID to KeyTestState.Valid,
            ApiKeyCheck.INVALID to KeyTestState.Invalid,
            ApiKeyCheck.UNREACHABLE to KeyTestState.Unreachable
        )
        for ((check, expected) in cases) {
            val viewModel = viewModel(
                FakeSettings(),
                clock = { 0L },
                validateGemini = { check }
            )
            backgroundScope.launch { viewModel.geminiTest.collect {} }
            advanceUntilIdle()

            viewModel.testGeminiKey("k")
            advanceUntilIdle()

            assertEquals(expected, viewModel.geminiTest.value)
        }
    }

    @Test
    fun `completeSetup marks flag`() = runTest {
        val settings = FakeSettings()
        val viewModel = viewModel(settings, clock = { 0L })

        viewModel.completeSetup()
        advanceUntilIdle()

        assertEquals(true, settings.setupComplete.value)
    }

    private fun viewModel(
        settings: FakeSettings,
        clock: () -> Long,
        testOllama: suspend () -> Boolean = { false },
        validateGemini: suspend (String) -> ApiKeyCheck = { ApiKeyCheck.UNREACHABLE },
        validateOpenRouter: suspend (String) -> ApiKeyCheck = { ApiKeyCheck.UNREACHABLE }
    ) = SettingsViewModel(
        settingsRepository = settings,
        isGeminiConfigured = false,
        isOpenRouterConfigured = false,
        appVersion = "0.1.0",
        testOllamaConnection = testOllama,
        clock = clock,
        observeUserKey = { kind -> settings.keys.getValue(kind) },
        saveUserKey = { kind, value -> settings.keys.getValue(kind).value = value },
        clearUserKey = { kind -> settings.keys.getValue(kind).value = "" },
        validateGeminiKey = validateGemini,
        validateOpenRouterKey = validateOpenRouter
    )

    private class FakeSettings : SettingsRepository {
        val theme = MutableStateFlow(AppTheme.SYSTEM)
        val provider = MutableStateFlow(AiProviderChoice.MOCK)
        val language = MutableStateFlow(AppLanguage.SYSTEM)
        val webSearch = MutableStateFlow(false)
        val ollamaUrl = MutableStateFlow("")
        val ollamaModel = MutableStateFlow("")
        val developerMode = MutableStateFlow(false)
        val setupComplete = MutableStateFlow(false)
        val keys = ApiKeyKind.entries.associateWith { MutableStateFlow("") }

        override fun observeTheme() = theme
        override suspend fun setTheme(theme: AppTheme) {
            this.theme.value = theme
        }

        override fun observeAiProvider() = provider
        override suspend fun setAiProvider(choice: AiProviderChoice) {
            provider.value = choice
        }

        override fun observeLanguage() = language
        override suspend fun setLanguage(language: AppLanguage) {
            this.language.value = language
        }

        override fun observeWebSearch() = webSearch
        override suspend fun setWebSearchEnabled(enabled: Boolean) {
            webSearch.value = enabled
        }

        override fun observeOllamaUrl() = ollamaUrl
        override suspend fun setOllamaUrl(url: String) {
            ollamaUrl.value = url
        }

        override fun observeOllamaModel() = ollamaModel
        override suspend fun setOllamaModel(model: String) {
            ollamaModel.value = model
        }

        override fun observeDeveloperMode() = developerMode
        override suspend fun setDeveloperMode(enabled: Boolean) {
            developerMode.value = enabled
        }

        override fun observeSetupComplete() = setupComplete
        override suspend fun setSetupComplete(completed: Boolean) {
            setupComplete.value = completed
        }
    }
}
