package com.curio.notes.ai.prompts

import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.ConversationContext
import com.curio.notes.domain.model.NoteType
import org.junit.Assert.assertEquals
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
            "examples", "keyPoints", "relatedTopics", "followUpQuestions"
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
            "examples", "keyPoints", "relatedTopics", "followUpQuestions"
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
}
