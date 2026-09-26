package com.curio.notes.di

import android.content.Context
import androidx.room.Room
import com.curio.notes.BuildConfig
import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.ApiKeyCheck
import com.curio.notes.ai.GenerationTracker
import com.curio.notes.ai.SwitchingAIProvider
import com.curio.notes.ai.providers.GeminiAIProvider
import com.curio.notes.ai.providers.MockAIProvider
import com.curio.notes.ai.providers.OllamaAIProvider
import com.curio.notes.ai.providers.OpenRouterAIProvider
import com.curio.notes.ai.search.TavilyWebSearch
import com.curio.notes.data.local.database.CurioDatabase
import com.curio.notes.data.local.database.MIGRATION_1_2
import com.curio.notes.data.local.database.MIGRATION_2_3
import com.curio.notes.data.local.database.MIGRATION_3_4
import com.curio.notes.data.local.database.MIGRATION_4_5
import com.curio.notes.data.preferences.SettingsRepositoryImpl
import com.curio.notes.data.preferences.settingsDataStore
import com.curio.notes.data.security.ApiKeyStorage
import com.curio.notes.domain.model.ApiKeyKind
import com.curio.notes.domain.model.resolveApiKey
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao(), database.conversationDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(appContext.settingsDataStore)
    }

    val apiKeyStorage: ApiKeyStorage by lazy { ApiKeyStorage(appContext) }

    // A provider counts as configured when either a user key was saved
    // (Settings/Setup) or the build carries a developer key. Evaluated per
    // call so saving a key updates Settings without an app restart.
    fun isGeminiConfigured(): Boolean = resolveApiKey(
        apiKeyStorage.getKey(ApiKeyKind.GEMINI), BuildConfig.GEMINI_API_KEY
    ).isNotBlank()

    fun isOpenRouterConfigured(): Boolean = resolveApiKey(
        apiKeyStorage.getKey(ApiKeyKind.OPENROUTER), BuildConfig.OPENROUTER_API_KEY
    ).isNotBlank()

    val appVersion: String = BuildConfig.VERSION_NAME

    private val okHttpClient: OkHttpClient by lazy {
        GeminiAIProvider.defaultClient()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Routes every AI call to the provider chosen in Settings. Mock is the
    // safe default; cloud providers use the user-saved key first and the
    // build-time key (local.properties) only as developer fallback.
    // Web search (Tavily) is opt-in per note/conversation turn: the providers
    // ask the gatekeeper first, and only search when it says so.
    // Ollama talks to the user's laptop on the local network (Developer options).
    private val tavilySearch: TavilyWebSearch by lazy {
        TavilyWebSearch(
            apiKey = BuildConfig.TAVILY_API_KEY,
            apiKeyOverride = { apiKeyStorage.getKey(ApiKeyKind.TAVILY) },
            client = okHttpClient
        )
    }

    private val ollamaProvider: OllamaAIProvider by lazy {
        OllamaAIProvider(
            serverUrl = settingsRepository.observeOllamaUrl(),
            modelName = settingsRepository.observeOllamaModel(),
            client = okHttpClient,
            language = settingsRepository.observeLanguage(),
            webSearch = tavilySearch,
            webSearchEnabled = settingsRepository.observeWebSearch()
        )
    }

    val testOllamaConnection: suspend () -> Boolean = {
        ollamaProvider.testConnection()
    }

    // Key checks for Settings/Setup. Lightweight instances sharing the app
    // OkHttp client; validateKey() only hits each provider's models endpoint.
    val validateGeminiKey: suspend (String) -> ApiKeyCheck = { key ->
        GeminiAIProvider(apiKey = "", client = okHttpClient).validateKey(key)
    }

    val validateOpenRouterKey: suspend (String) -> ApiKeyCheck = { key ->
        OpenRouterAIProvider(apiKey = "", client = okHttpClient).validateKey(key)
    }

    val aiProvider: AIProvider by lazy {
        val searchEnabled = settingsRepository.observeWebSearch()
        SwitchingAIProvider(
            settingsRepository = settingsRepository,
            scope = applicationScope,
            mock = MockAIProvider(
                language = settingsRepository.observeLanguage(),
                webSearchEnabled = searchEnabled
            ),
            gemini = GeminiAIProvider(
                apiKey = BuildConfig.GEMINI_API_KEY,
                apiKeyOverride = { apiKeyStorage.getKey(ApiKeyKind.GEMINI) },
                client = okHttpClient,
                language = settingsRepository.observeLanguage(),
                webSearch = tavilySearch,
                webSearchEnabled = searchEnabled
            ),
            openRouter = OpenRouterAIProvider(
                apiKey = BuildConfig.OPENROUTER_API_KEY,
                apiKeyOverride = { apiKeyStorage.getKey(ApiKeyKind.OPENROUTER) },
                client = okHttpClient,
                language = settingsRepository.observeLanguage(),
                webSearch = tavilySearch,
                webSearchEnabled = searchEnabled
            ),
            ollama = ollamaProvider
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
        ProcessNoteUseCase(noteRepository, aiProvider, applicationScope, generationTracker)
    }

    val generationTracker: GenerationTracker by lazy {
        GenerationTracker(settingsRepository, applicationScope)
    }
}
