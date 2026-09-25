package com.curio.notes.ai

import com.curio.notes.domain.model.AiProviderChoice
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.AppTheme
import com.curio.notes.domain.model.NoteType
import com.curio.notes.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SwitchingAIProviderTest {

    @Test
    fun `routes to mock by default and gemini after switch`() = runTest {
        val settings = FakeSettingsRepository()
        // Unconfined scope: emissions propagate synchronously, no virtual time needed.
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        try {
            val router = SwitchingAIProvider(
                settingsRepository = settings,
                scope = scope,
                mock = RecordingProvider("mock"),
                gemini = RecordingProvider("gemini"),
                openRouter = RecordingProvider("openrouter")
            )

            assertEquals("mock: hi", router.classifyAndAnswer("hi").title)

            settings.emit(AiProviderChoice.GEMINI)

            assertEquals("gemini: hi", router.classifyAndAnswer("hi").title)
            val context = ConversationContext("orig", null, null)
            assertEquals("gemini-reply", router.continueConversation(context, emptyList(), "q"))

            settings.emit(AiProviderChoice.OPENROUTER)

            assertEquals("openrouter: hi", router.classifyAndAnswer("hi").title)
        } finally {
            scope.cancel()
        }
    }

    private class RecordingProvider(private val tag: String) : AIProvider {
        override suspend fun classifyAndAnswer(input: String): AIResponse =
            AIResponse(type = NoteType.OTHER, title = "$tag: $input")

        override suspend fun continueConversation(
            context: ConversationContext,
            history: List<ChatMessage>,
            input: String
        ): String = "$tag-reply"
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val theme = MutableStateFlow(AppTheme.SYSTEM)
        private val provider = MutableStateFlow(AiProviderChoice.MOCK)
        private val language = MutableStateFlow(AppLanguage.SYSTEM)
        private val webSearch = MutableStateFlow(false)

        fun emit(choice: AiProviderChoice) {
            provider.value = choice
        }

        override fun observeTheme() = theme

        override suspend fun setTheme(theme: AppTheme) {
            this.theme.value = theme
        }

        override fun observeAiProvider() = provider

        override suspend fun setAiProvider(choice: AiProviderChoice) {
            provider.value = choice
        }

        override fun observeLanguage() = language

        override suspend fun setLanguage(language: AppLanguage) {
            this.language.value = language
        }

        override fun observeWebSearch() = webSearch

        override suspend fun setWebSearchEnabled(enabled: Boolean) {
            webSearch.value = enabled
        }
    }
}
