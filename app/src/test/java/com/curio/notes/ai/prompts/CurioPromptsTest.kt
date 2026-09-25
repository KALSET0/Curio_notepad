package com.curio.notes.ai.prompts

import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.ConversationContext
import com.curio.notes.ai.search.WebResult
import com.curio.notes.domain.model.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurioPromptsTest {

    @Test
    fun `every note type is defined`() {
        for (type in NoteType.entries) {
            assertTrue(
                "SYSTEM_PROMPT does not define $type",
                CurioPrompts.SYSTEM_PROMPT.contains(type.name)
            )
        }
    }

    @Test
    fun `every json key is specified`() {
        val keys = listOf(
            "type", "title", "summary", "explanation",
            "examples", "keyPoints", "relatedTopics", "followUpQuestions",
            "sources"
        )
        for (key in keys) {
            assertTrue(
                "SYSTEM_PROMPT does not specify \"$key\"",
                CurioPrompts.SYSTEM_PROMPT.contains("\"$key\"")
            )
        }
    }

    @Test
    fun `output rules forbid markup and demand json`() {
        val prompt = CurioPrompts.SYSTEM_PROMPT
        assertTrue(prompt.contains("JSON"))
        assertTrue(prompt.lowercase().contains("markdown"))
        assertTrue(prompt.contains("Disambiguation:"))
    }

    @Test
    fun `honesty rules are present`() {
        val prompt = CurioPrompts.SYSTEM_PROMPT.lowercase()
        assertTrue(prompt.contains("never invent facts"))
        assertTrue(prompt.contains("uncertain"))
        assertTrue(prompt.contains("preserve"))
    }

    @Test
    fun `user prompt embeds the thought and demands schema json`() {
        val input = "Why is the sky blue?"
        val prompt = CurioPrompts.userPromptFor(input)
        assertTrue(prompt.contains(input))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun `conversation context carries note details`() {
        val context = ConversationContext(
            originalText = "Why?",
            type = NoteType.QUESTION,
            summary = "Because.",
            relatedTopics = listOf("T1", "T2")
        )
        val text = CurioPrompts.conversationContextFor(context)
        assertTrue(text.contains("Why?"))
        assertTrue(text.contains("QUESTION"))
        assertTrue(text.contains("Because."))
        assertTrue(text.contains("T1"))
    }

    @Test
    fun `continuation system demands prose`() {
        val system = CurioPrompts.CONTINUATION_SYSTEM.lowercase()
        assertTrue(system.contains("plain text"))
        assertTrue(system.contains("follow-up"))
    }

    @Test
    fun `english helpers stay the default`() {
        assertEquals(CurioPrompts.SYSTEM_PROMPT, CurioPrompts.systemPromptFor(AiLanguage.ENGLISH))
        assertEquals(
            CurioPrompts.CONTINUATION_SYSTEM,
            CurioPrompts.continuationSystemFor(AiLanguage.ENGLISH)
        )
    }

    @Test
    fun `spanish system prompt keeps schema and demands spanish`() {
        val prompt = CurioPrompts.systemPromptFor(AiLanguage.SPANISH)
        val keys = listOf(
            "type", "title", "summary", "explanation",
            "examples", "keyPoints", "relatedTopics", "followUpQuestions",
            "sources"
        )
        for (key in keys) {
            assertTrue("ES prompt does not specify \"$key\"", prompt.contains("\"$key\""))
        }
        for (type in NoteType.entries) {
            assertTrue("ES prompt does not define $type", prompt.contains(type.name))
        }
        assertTrue(prompt.lowercase().contains("espa"))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun `spanish user prompt embeds the thought`() {
        val input = "¿Por qué el cielo es azul?"
        val prompt = CurioPrompts.userPromptFor(input, AiLanguage.SPANISH)
        assertTrue(prompt.contains(input))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun `spanish conversation context is translated`() {
        val context = ConversationContext(
            originalText = "¿Por qué?",
            type = NoteType.QUESTION,
            summary = "Porque sí.",
            relatedTopics = listOf("T1")
        )
        val text = CurioPrompts.conversationContextFor(context, AiLanguage.SPANISH)
        assertTrue(text.contains("¿Por qué?"))
        assertTrue(text.contains("QUESTION"))
        assertTrue(text.contains("Porque sí."))
        assertTrue(text.contains("Tipo detectado"))
    }

    @Test
    fun `follow-up questions must read as the user's own likely doubts`() {
        val english = CurioPrompts.systemPromptFor(AiLanguage.ENGLISH)
        assertTrue(english.contains("plausibly ask"))
        assertTrue(english.contains("user's own"))

        val spanish = CurioPrompts.systemPromptFor(AiLanguage.SPANISH)
        assertTrue(spanish.contains("propio usuario"))
        assertTrue(spanish.contains("dudas probables"))
    }

    @Test
    fun `spanish continuation system demands spanish prose`() {
        val system = CurioPrompts.continuationSystemFor(AiLanguage.SPANISH).lowercase()
        assertTrue(system.contains("espa"))
        assertTrue(system.contains("texto plano"))
    }

    @Test
    fun `gatekeeper demands a boolean decision plus query`() {
        for (language in AiLanguage.entries) {
            val system = CurioPrompts.gatekeeperSystemFor(language)
            assertTrue(system.contains("\"need_search\""))
            assertTrue(system.contains("\"query\""))
            assertTrue(system.contains("JSON"))
        }
        assertTrue(
            CurioPrompts.gatekeeperSystemFor(AiLanguage.SPANISH).lowercase()
                .contains("idioma del usuario")
        )
    }

    @Test
    fun `gatekeeper prompts embed the input`() {
        val prompt = CurioPrompts.gatekeeperPromptFor("¿Es verdad X?", AiLanguage.SPANISH)
        assertTrue(prompt.contains("¿Es verdad X?"))
        assertTrue(prompt.contains("JSON"))

        val context = ConversationContext(
            originalText = "Coffee",
            type = NoteType.CLAIM,
            summary = "Claimed.",
            relatedTopics = listOf("T1")
        )
        val convo = CurioPrompts.gatekeeperPromptForConversation(
            context, "Is it really true?", AiLanguage.ENGLISH
        )
        assertTrue(convo.contains("Coffee"))
        assertTrue(convo.contains("Is it really true?"))
        assertTrue(convo.contains("JSON"))
    }

    @Test
    fun `sources are injected with anti-hallucination instruction`() {
        val results = listOf(WebResult("T", "https://example.com/a", "snippet"))
        val prompt = CurioPrompts.userPromptFor("Hi?", AiLanguage.ENGLISH, results)
        assertTrue(prompt.contains("https://example.com/a"))
        assertTrue(prompt.lowercase().contains("never invent"))

        val plain = CurioPrompts.userPromptFor("Hi?", AiLanguage.ENGLISH)
        assertFalse(plain.contains("example.com"))

        val convo = CurioPrompts.conversationContextFor(
            ConversationContext("Hi", null, null), AiLanguage.SPANISH, results
        )
        assertTrue(convo.contains("https://example.com/a"))
    }
}
