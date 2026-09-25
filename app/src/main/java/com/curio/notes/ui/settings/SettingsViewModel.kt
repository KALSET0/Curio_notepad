package com.curio.notes.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    val isGeminiConfigured: Boolean,
    val isOpenRouterConfigured: Boolean,
    val appVersion: String
) : ViewModel() {
    val theme: StateFlow<AppTheme> = settingsRepository.observeTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppTheme.SYSTEM)

    val aiProvider: StateFlow<AiProviderChoice> = settingsRepository.observeAiProvider()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiProviderChoice.MOCK)

    val language: StateFlow<AppLanguage> = settingsRepository.observeLanguage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setAiProvider(choice: AiProviderChoice) {
        viewModelScope.launch { settingsRepository.setAiProvider(choice) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    companion object {
        fun factory(
            repository: SettingsRepository,
            isGeminiConfigured: Boolean,
            isOpenRouterConfigured: Boolean,
            appVersion: String
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(
                        repository,
                        isGeminiConfigured,
                        isOpenRouterConfigured,
                        appVersion
                    ) as T
            }
    }
}
