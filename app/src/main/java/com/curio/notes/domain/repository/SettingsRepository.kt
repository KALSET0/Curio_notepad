package com.curio.notes.domain.repository

import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
    fun observeAiProvider(): Flow<AiProviderChoice>
    suspend fun setAiProvider(choice: AiProviderChoice)
}
