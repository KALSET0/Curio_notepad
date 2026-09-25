package com.curio.notes.di

import android.content.Context
import androidx.room.Room
import com.curio.notes.BuildConfig
import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.SwitchingAIProvider
import com.curio.notes.ai.providers.GeminiAIProvider
import com.curio.notes.ai.providers.MockAIProvider
import com.curio.notes.ai.providers.OpenRouterAIProvider
import com.curio.notes.data.local.database.CurioDatabase
import com.curio.notes.data.local.database.MIGRATION_1_2
import com.curio.notes.data.local.database.MIGRATION_2_3
import com.curio.notes.data.local.database.MIGRATION_3_4
import com.curio.notes.data.preferences.SettingsRepositoryImpl
import com.curio.notes.data.preferences.settingsDataStore
import com.curio.notes.data.repository.NoteRepositoryImpl
import com.curio.notes.domain.repository.NoteRepository
import com.curio.notes.domain.repository.SettingsRepository
import com.curio.notes.domain.usecase.ProcessNoteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: CurioDatabase by lazy {
        Room.databaseBuilder(appContext, CurioDatabase::class.java, "curio.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao(), database.conversationDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(appContext.settingsDataStore)
    }

    val isGeminiConfigured: Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    val isOpenRouterConfigured: Boolean = BuildConfig.OPENROUTER_API_KEY.isNotBlank()

    val appVersion: String = BuildConfig.VERSION_NAME

    private val okHttpClient: OkHttpClient by lazy {
        GeminiAIProvider.defaultClient()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Routes every AI call to the provider chosen in Settings. Mock is the
    // safe default; Gemini needs GEMINI_API_KEY in local.properties.
    val aiProvider: AIProvider by lazy {
        SwitchingAIProvider(
            settingsRepository = settingsRepository,
            scope = applicationScope,
            mock = MockAIProvider(),
            gemini = GeminiAIProvider(
                apiKey = BuildConfig.GEMINI_API_KEY,
                client = okHttpClient
            ),
            openRouter = OpenRouterAIProvider(
                apiKey = BuildConfig.OPENROUTER_API_KEY,
                client = okHttpClient
            )
        )
    }

    init {
        // Crash-safe resume: notes stuck in PROCESSING go back to PENDING,
        // and anything never attempted gets enqueued. A failure here must
        // never prevent the app from opening.
        applicationScope.launch {
            runCatching { processNoteUseCase.resumePending() }
        }
    }

    val processNoteUseCase: ProcessNoteUseCase by lazy {
        ProcessNoteUseCase(noteRepository, aiProvider, applicationScope)
    }
}
