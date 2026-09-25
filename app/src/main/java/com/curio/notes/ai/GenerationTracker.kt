package com.curio.notes.ai

import android.os.SystemClock
import com.curio.notes.ai.providers.GeminiConfig
import com.curio.notes.ai.providers.OLLAMA_DEFAULT_MODEL
import com.curio.notes.ai.providers.OpenRouterConfig
import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

data class GenerationInfo(
    val label: String,
    val durationMillis: Long
)

fun formatGenerationDuration(millis: Long): String =
    String.format(Locale.US, "%.1f s", millis / 1000.0)

fun providerDisplayLabel(choice: AiProviderChoice, ollamaModel: String): String =
    when (choice) {
        AiProviderChoice.MOCK -> "Mock AI"
        AiProviderChoice.GEMINI -> "Gemini · ${GeminiConfig.MODEL}"
        AiProviderChoice.OPENROUTER -> "OpenRouter · ${OpenRouterConfig.MODEL}"
        AiProviderChoice.OLLAMA -> "Ollama · ${ollamaModel.ifBlank { OLLAMA_DEFAULT_MODEL }}"
    }

/**
 * Developer-mode telemetry: measures end-to-end generation time (including
 * gatekeeper and web search, which is the wait the user actually feels)
 * and resolves the provider label. In-memory per note; the durable copy
 * lives on the note / message rows themselves.
 */
class GenerationTracker(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() }
) {
    val developerMode: StateFlow<Boolean> = settingsRepository.observeDeveloperMode()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val currentLabel: StateFlow<String> = combine(
        settingsRepository.observeAiProvider(),
        settingsRepository.observeOllamaModel()
    ) { choice, model -> providerDisplayLabel(choice, model) }
        .stateIn(scope, SharingStarted.Eagerly, providerDisplayLabel(AiProviderChoice.MOCK, ""))

    private val _byNote = MutableStateFlow(mapOf<Long, GenerationInfo>())

    fun statsFor(noteId: Long): GenerationInfo? = _byNote.value[noteId]

    suspend fun <T> track(noteId: Long, block: suspend () -> T): T {
        val start = clock()
        try {
            return block()
        } finally {
            _byNote.value += noteId to GenerationInfo(currentLabel.value, clock() - start)
        }
    }
}
