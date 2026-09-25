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
    private val openRouter: AIProvider,
    private val ollama: AIProvider
) : AIProvider {
    private val choice: StateFlow<AiProviderChoice> = settingsRepository.observeAiProvider()
        .stateIn(scope, SharingStarted.Eagerly, AiProviderChoice.MOCK)

    private val developerMode: StateFlow<Boolean> = settingsRepository.observeDeveloperMode()
        .stateIn(scope, SharingStarted.Eagerly, false)

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
        // The local server hides with developer mode: selecting it while
        // hidden (or after switching off) safely falls back to Mock.
        AiProviderChoice.OLLAMA -> if (developerMode.value) ollama else mock
    }
}
