package com.curio.notes.ai.providers

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ConversationContext
import kotlinx.coroutines.delay
import java.io.IOException

class MockAIProvider(
    private val delayMillis: Long = 1_500L
) : AIProvider {
    override suspend fun classifyAndAnswer(input: String): AIResponse {
        delay(delayMillis)
        if (input.contains("mock-error", ignoreCase = true)) {
            throw IOException("Mock AI forced failure (test hook: note contains \"mock-error\")")
        }
        return mockResponseFor(guessNoteType(input), input)
    }

    override suspend fun continueConversation(
        context: ConversationContext,
        history: List<ChatMessage>,
        input: String
    ): String {
        delay(800)
        val topic = context.summary?.take(80)?.ifBlank { null }
            ?: topicOf(context.originalText)
        return "Mock reply to \"$input\": a real provider would answer from " +
            "this note (\"$topic\") using ${history.size} earlier messages as context."
    }
}
