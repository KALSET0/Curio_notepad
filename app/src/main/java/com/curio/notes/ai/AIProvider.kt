package com.curio.notes.ai

interface AIProvider {
    suspend fun classifyAndAnswer(input: String): AIResponse

    suspend fun continueConversation(
        context: ConversationContext,
        history: List<ChatMessage>,
        input: String
    ): String

    // Fresh follow-up question ideas for the current conversation state.
    // Never throws structured-data guarantees: empty list means "keep old".
    suspend fun suggestFollowUps(
        context: ConversationContext,
        history: List<ChatMessage>
    ): List<String>
}
