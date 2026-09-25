package com.curio.notes.ui.settings

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OllamaTestState {
    data object Idle : OllamaTestState
    data object Checking : OllamaTestState
    data object Ok : OllamaTestState
    data object Failed : OllamaTestState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    val isGeminiConfigured: Boolean,
    val isOpenRouterConfigured: Boolean,
    val appVersion: String,
    private val testOllamaConnection: suspend () -> Boolean = { false },
    private val clock: () -> Long = { SystemClock.elapsedRealtime() }
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

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDeveloperMode(enabled)
            if (!enabled && aiProvider.value == AiProviderChoice.OLLAMA) {
                settingsRepository.setAiProvider(AiProviderChoice.MOCK)
            }
        }
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
            testOllamaConnection: suspend () -> Boolean = { false }
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(
                        repository,
                        isGeminiConfigured,
                        isOpenRouterConfigured,
                        appVersion,
                        testOllamaConnection
                    ) as T
            }
    }
}
