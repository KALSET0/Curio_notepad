package com.curio.notes.ai.providers

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.AiSource
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ConversationContext
import com.curio.notes.domain.model.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

class MockAIProvider(
    private val delayMillis: Long = 1_500L,
    private val language: Flow<AppLanguage> = flowOf(AppLanguage.ENGLISH),
    private val webSearchEnabled: Flow<Boolean> = flowOf(false)
) : AIProvider {
    override suspend fun classifyAndAnswer(input: String): AIResponse {
        delay(delayMillis)
        if (input.contains("mock-error", ignoreCase = true)) {
            throw IOException("Mock AI forced failure (test hook: note contains \"mock-error\")")
        }
        val aiLanguage = language.first().toAiLanguage()
        val response = mockResponseFor(guessNoteType(input), input, aiLanguage)
        if (!webSearchEnabled.first() || !mockNeedsSearch(input)) return response
        return response.copy(sources = mockExampleSources(aiLanguage))
    }

    override suspend fun continueConversation(
        context: ConversationContext,
        history: List<ChatMessage>,
        input: String
    ): String {
        delay(800)
        val aiLanguage = language.first().toAiLanguage()
        val topic = context.summary?.take(80)?.ifBlank { null }
            ?: topicOf(context.originalText)
        // The gatekeeper decides once, on the first turn.
        val searched = history.isEmpty() &&
            webSearchEnabled.first() &&
            mockNeedsSearch(input)
        val base = if (aiLanguage == AiLanguage.SPANISH) {
            "Respuesta simulada a \"$input\": un proveedor real respondería desde " +
                "esta nota (\"$topic\") usando ${history.size} mensajes anteriores como contexto."
        } else {
            "Mock reply to \"$input\": a real provider would answer from " +
                "this note (\"$topic\") using ${history.size} earlier messages as context."
        }
        if (!searched) return base
        return base + if (aiLanguage == AiLanguage.SPANISH) {
            " (Mock también consultó 2 fuentes web de ejemplo.)"
        } else {
            " (Mock also consulted 2 example web sources.)"
        }
    }
}

internal fun mockNeedsSearch(input: String): Boolean {
    val text = input.lowercase()
    if (text.contains("mock-search")) return true
    val triggers = listOf(
        "latest", "current", "news", "today", "price",
        "2024", "2025", "2026",
        "hoy", "actual", "noticia", "precio",
        "últim", "ultim", "verdad", "récord", "record",
        "elecci", "guerra", "clima", "dólar", "dolar"
    )
    return triggers.any { text.contains(it) }
}

internal fun mockExampleSources(language: AiLanguage): List<AiSource> =
    if (language == AiLanguage.SPANISH) {
        listOf(
            AiSource("Fuente simulada: artículo de ejemplo", "https://example.com/article"),
            AiSource("Fuente simulada: contexto de ejemplo", "https://example.com/background")
        )
    } else {
        listOf(
            AiSource("Mock source: example article", "https://example.com/article"),
            AiSource("Mock source: example background", "https://example.com/background")
        )
    }
