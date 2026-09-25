package com.curio.notes.ai

interface AIProvider {
    suspend fun classifyAndAnswer(input: String): AIResponse

    suspend fun continueConversation(
        context: ConversationContext,
        history: List<ChatMessage>,
        input: String
    ): String
}
