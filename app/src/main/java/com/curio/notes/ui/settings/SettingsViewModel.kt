package com.curio.notes.ui.settings

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.ai.ApiKeyCheck
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.ApiKeyKind
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OllamaTestState {
    data object Idle : OllamaTestState
    data object Checking : OllamaTestState
    data object Ok : OllamaTestState
    data object Failed : OllamaTestState
}

// Result of a "Test key" tap. Five states on purpose: the UI must tell a
// rejected key apart from no connectivity, and never claim "valid" untested.
sealed interface KeyTestState {
    data object Idle : KeyTestState
    data object Checking : KeyTestState
    data object Valid : KeyTestState
    data object Invalid : KeyTestState
    data object Unreachable : KeyTestState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    val isGeminiConfigured: Boolean,
    val isOpenRouterConfigured: Boolean,
    val appVersion: String,
    private val testOllamaConnection: suspend () -> Boolean = { false },
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val observeUserKey: (ApiKeyKind) -> Flow<String> = { flowOf("") },
    private val saveUserKey: (ApiKeyKind, String) -> Unit = { _, _ -> },
    private val clearUserKey: (ApiKeyKind) -> Unit = {},
    private val validateGeminiKey: suspend (String) -> ApiKeyCheck = { ApiKeyCheck.UNREACHABLE },
    private val validateOpenRouterKey: suspend (String) -> ApiKeyCheck = { ApiKeyCheck.UNREACHABLE }
) : ViewModel() {
    val theme: StateFlow<AppTheme> = settingsRepository.observeTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppTheme.SYSTEM)

    val aiProvider: StateFlow<AiProviderChoice> = settingsRepository.observeAiProvider()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiProviderChoice.MOCK)

    val language: StateFlow<AppLanguage> = settingsRepository.observeLanguage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)

    val webSearchEnabled: StateFlow<Boolean> = settingsRepository.observeWebSearch()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val ollamaUrl: StateFlow<String> = settingsRepository.observeOllamaUrl()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val ollamaModel: StateFlow<String> = settingsRepository.observeOllamaModel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val developerMode: StateFlow<Boolean> = settingsRepository.observeDeveloperMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _ollamaTest = MutableStateFlow<OllamaTestState>(OllamaTestState.Idle)
    val ollamaTest: StateFlow<OllamaTestState> = _ollamaTest

    // User-entered keys (encrypted storage). Empty = not configured by user.
    val geminiKey: StateFlow<String> = observeUserKey(ApiKeyKind.GEMINI)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val openRouterKey: StateFlow<String> = observeUserKey(ApiKeyKind.OPENROUTER)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val tavilyKey: StateFlow<String> = observeUserKey(ApiKeyKind.TAVILY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _geminiTest = MutableStateFlow<KeyTestState>(KeyTestState.Idle)
    val geminiTest: StateFlow<KeyTestState> = _geminiTest
    private val _openRouterTest = MutableStateFlow<KeyTestState>(KeyTestState.Idle)
    val openRouterTest: StateFlow<KeyTestState> = _openRouterTest

    private val versionTaps = mutableListOf<Long>()

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setAiProvider(choice: AiProviderChoice) {
        viewModelScope.launch { settingsRepository.setAiProvider(choice) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setWebSearchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWebSearchEnabled(enabled) }
    }

    fun setOllamaUrl(url: String) {
        _ollamaTest.value = OllamaTestState.Idle
        viewModelScope.launch { settingsRepository.setOllamaUrl(url.trim()) }
    }

    fun setOllamaModel(model: String) {
        viewModelScope.launch { settingsRepository.setOllamaModel(model.trim()) }
    }

    // User API keys: saved encrypted on device, never logged, never shown
    // in full. Saving resets that provider's test state (re-test explicitly).
    fun saveApiKey(kind: ApiKeyKind, value: String) {
        saveUserKey(kind, value)
        resetKeyTest(kind)
    }

    fun clearApiKey(kind: ApiKeyKind) {
        clearUserKey(kind)
        resetKeyTest(kind)
    }

    fun resetKeyTest(kind: ApiKeyKind) {
        when (kind) {
            ApiKeyKind.GEMINI -> _geminiTest.value = KeyTestState.Idle
            ApiKeyKind.OPENROUTER -> _openRouterTest.value = KeyTestState.Idle
            ApiKeyKind.TAVILY -> Unit
        }
    }

    fun testGeminiKey(key: String) = testKey(
        key = key,
        state = _geminiTest,
        validate = validateGeminiKey
    )

    fun testOpenRouterKey(key: String) = testKey(
        key = key,
        state = _openRouterTest,
        validate = validateOpenRouterKey
    )

    private fun testKey(
        key: String,
        state: MutableStateFlow<KeyTestState>,
        validate: suspend (String) -> ApiKeyCheck
    ) {
        if (state.value == KeyTestState.Checking) return
        state.value = KeyTestState.Checking
        viewModelScope.launch {
            state.value = try {
                when (validate(key)) {
                    ApiKeyCheck.VALID -> KeyTestState.Valid
                    ApiKeyCheck.INVALID -> KeyTestState.Invalid
                    ApiKeyCheck.UNREACHABLE -> KeyTestState.Unreachable
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                KeyTestState.Unreachable
            }
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDeveloperMode(enabled)
            if (!enabled && aiProvider.value == AiProviderChoice.OLLAMA) {
                settingsRepository.setAiProvider(AiProviderChoice.MOCK)
            }
        }
    }

    // First-run setup completion (Save and Skip both land here).
    fun completeSetup() {
        viewModelScope.launch { settingsRepository.setSetupComplete(true) }
    }

    // Hidden unlock: tapping the version enough times inside a short window
    // enables developer mode. Tapping more while on does nothing; turning
    // off lives in Developer options.
    fun onVersionTap() {
        val now = clock()
        versionTaps.removeAll { now - it > DEV_UNLOCK_WINDOW_MILLIS }
        versionTaps += now
        if (versionTaps.size >= DEV_UNLOCK_TAPS && !developerMode.value) {
            versionTaps.clear()
            setDeveloperMode(true)
        }
    }

    fun testOllama() {
        if (_ollamaTest.value == OllamaTestState.Checking) return
        _ollamaTest.value = OllamaTestState.Checking
        viewModelScope.launch {
            _ollamaTest.value = try {
                if (testOllamaConnection()) {
                    OllamaTestState.Ok
                } else {
                    OllamaTestState.Failed
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                OllamaTestState.Failed
            }
        }
    }

    companion object {
        const val DEV_UNLOCK_TAPS = 5
        const val DEV_UNLOCK_WINDOW_MILLIS = 2_000L

        fun factory(
            repository: SettingsRepository,
            isGeminiConfigured: Boolean,
            isOpenRouterConfigured: Boolean,
            appVersion: String,
            testOllamaConnection: suspend () -> Boolean = { false },
            observeUserKey: (ApiKeyKind) -> Flow<String> = { flowOf("") },
            saveUserKey: (ApiKeyKind, String) -> Unit = { _, _ -> },
            clearUserKey: (ApiKeyKind) -> Unit = {},
            validateGeminiKey: suspend (String) -> ApiKeyCheck = { ApiKeyCheck.UNREACHABLE },
            validateOpenRouterKey: suspend (String) -> ApiKeyCheck = { ApiKeyCheck.UNREACHABLE }
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(
                        repository,
                        isGeminiConfigured,
                        isOpenRouterConfigured,
                        appVersion,
                        testOllamaConnection,
                        observeUserKey = observeUserKey,
                        saveUserKey = saveUserKey,
                        clearUserKey = clearUserKey,
                        validateGeminiKey = validateGeminiKey,
                        validateOpenRouterKey = validateOpenRouterKey
                    ) as T
            }
    }
}
