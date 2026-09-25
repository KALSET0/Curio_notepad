package com.curio.notes.ai

import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SwitchingAIProvider(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    private val mock: AIProvider,
    private val gemini: AIProvider,
    private val openRouter: AIProvider
) : AIProvider {
    private val choice: StateFlow<AiProviderChoice> = settingsRepository.observeAiProvider()
        .stateIn(scope, SharingStarted.Eagerly, AiProviderChoice.MOCK)

    override suspend fun classifyAndAnswer(input: String): AIResponse =
        current().classifyAndAnswer(input)

    override suspend fun continueConversation(
        context: ConversationContext,
        history: List<ChatMessage>,
        input: String
    ): String = current().continueConversation(context, history, input)

    private fun current(): AIProvider = when (choice.value) {
        AiProviderChoice.MOCK -> mock
        AiProviderChoice.GEMINI -> gemini
        AiProviderChoice.OPENROUTER -> openRouter
    }
}
