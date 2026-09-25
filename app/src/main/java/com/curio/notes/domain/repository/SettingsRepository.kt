package com.curio.notes.domain.repository

import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
    fun observeAiProvider(): Flow<AiProviderChoice>
    suspend fun setAiProvider(choice: AiProviderChoice)
    fun observeLanguage(): Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)
    fun observeWebSearch(): Flow<Boolean>
    suspend fun setWebSearchEnabled(enabled: Boolean)
    fun observeOllamaUrl(): Flow<String>
    suspend fun setOllamaUrl(url: String)
    fun observeOllamaModel(): Flow<String>
    suspend fun setOllamaModel(model: String)
    fun observeDeveloperMode(): Flow<Boolean>
    suspend fun setDeveloperMode(enabled: Boolean)
}
