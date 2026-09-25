package com.curio.notes.ai

import com.curio.notes.ai.providers.MockAIProvider
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.NoteType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class MockAIProviderTest {
    private val provider = MockAIProvider(delayMillis = 0L)

    @Test
    fun `classifies all eight note types`() = runTest {
        val cases = mapOf(
            "What is quantum entanglement?" to NoteType.QUESTION,
            "Is it true that coffee stunts growth" to NoteType.CLAIM,
            "I don't understand how taxes work" to NoteType.CONFUSION,
            "What if we built a habit tracker for friends" to NoteType.IDEA,
            "I think remote work changed how I read" to NoteType.REFLECTION,
            "I want to research urban gardening" to NoteType.TOPIC,
            "Define photosynthesis" to NoteType.CONCEPT,
            "Buy milk tomorrow" to NoteType.OTHER
        )
        for ((input, expected) in cases) {
            assertEquals("input: $input", expected, provider.classifyAndAnswer(input).type)
        }
    }

    @Test
    fun `every response is fully structured`() = runTest {
        val inputs = listOf(
            "What is quantum entanglement?",
            "Define photosynthesis",
            "What if we built a habit tracker",
            "I don't understand taxes",
            "I want to research gardening",
            "Is it true coffee helps",
            "I think mornings are better",
            "Buy milk tomorrow"
        )
        for (input in inputs) {
            val response = provider.classifyAndAnswer(input)
            assertTrue("title blank for: $input", response.title.isNotBlank())
            assertTrue("summary blank for: $input", !response.summary.isNullOrBlank())
            assertTrue("explanation blank for: $input", !response.explanation.isNullOrBlank())
            assertTrue("examples empty for: $input", response.examples.isNotEmpty())
            assertTrue("keyPoints empty for: $input", response.keyPoints.isNotEmpty())
            assertTrue("relatedTopics empty for: $input", response.relatedTopics.isNotEmpty())
            assertTrue("followUpQuestions empty for: $input", response.followUpQuestions.isNotEmpty())
        }
    }

    @Test
    fun `mock-error hook throws IOException`() = runTest {
        try {
            provider.classifyAndAnswer("This note contains mock-error on purpose")
            fail("Expected IOException")
        } catch (e: IOException) {
            // expected test hook
        }
    }

    @Test
    fun `conversation replies reference input and context`() = runTest {
        val context = ConversationContext(
            originalText = "Define photosynthesis",
            type = NoteType.CONCEPT,
            summary = "Plants make food.",
            relatedTopics = listOf("Chlorophyll")
        )
        val history = listOf(ChatMessage(ChatRole.USER, "Hi"))
        val reply = provider.continueConversation(context, history, "Tell me more")

        assertTrue(reply.contains("Tell me more"))
        assertTrue(reply.contains("1"))
    }

    @Test
    fun `very long input processes without issues`() = runTest {
        val input = "word ".repeat(20_001).trim()
        assertTrue(input.length > 100_000)

        val response = provider.classifyAndAnswer(input)

        assertEquals(NoteType.OTHER, response.type)
        assertTrue(response.title.isNotBlank())
        assertTrue(response.title.length <= 70)
    }

    @Test
    fun `spanish input classifies and answers in spanish`() = runTest {
        val spanish = MockAIProvider(
            delayMillis = 0L,
            language = flowOf(AppLanguage.SPANISH)
        )
        val cases = mapOf(
            "¿Qué es la fotosíntesis?" to NoteType.QUESTION,
            "No entiendo cómo funcionan los impuestos" to NoteType.CONFUSION,
            "Qué pasaría si construimos un rastreador de hábitos" to NoteType.IDEA,
            "Creo que el trabajo remoto cambió mi lectura" to NoteType.REFLECTION,
            "Quiero investigar la jardinería urbana" to NoteType.TOPIC,
            "¿Es verdad que el café frena el crecimiento?" to NoteType.QUESTION,
            "Define la fotosíntesis" to NoteType.CONCEPT
        )
        for ((input, expected) in cases) {
            val response = spanish.classifyAndAnswer(input)
            assertEquals("input: $input", expected, response.type)
            assertTrue(
                "summary not spanish for: $input",
                response.summary!!.lowercase().contains("simulad")
            )
        }
    }

    @Test
    fun `spanish conversation reply is translated`() = runTest {
        val spanish = MockAIProvider(
            delayMillis = 0L,
            language = flowOf(AppLanguage.SPANISH)
        )
        val context = ConversationContext(
            originalText = "Define la fotosíntesis",
            type = NoteType.CONCEPT,
            summary = "Las plantas producen alimento.",
            relatedTopics = listOf("Clorofila")
        )
        val reply = spanish.continueConversation(
            context,
            listOf(ChatMessage(ChatRole.USER, "Hola")),
            "Cuéntame más"
        )

        assertTrue(reply.contains("Cuéntame más"))
        assertTrue(reply.lowercase().contains("simulada"))
    }

    @Test
    fun `mock-search hook attaches example sources when enabled`() = runTest {
        val searching = MockAIProvider(
            delayMillis = 0L,
            webSearchEnabled = flowOf(true)
        )

        val response = searching.classifyAndAnswer("Buy milk mock-search")

        assertEquals(2, response.sources.size)
        assertTrue(response.sources.all { it.url.startsWith("https://example.com/") })
    }

    @Test
    fun `search disabled means no sources even with hook`() = runTest {
        val response = provider.classifyAndAnswer("Buy milk mock-search")

        assertTrue(response.sources.isEmpty())
    }

    @Test
    fun `conversation searches only on the first turn`() = runTest {
        val searching = MockAIProvider(
            delayMillis = 0L,
            webSearchEnabled = flowOf(true)
        )
        val context = ConversationContext("Latest news today", null, null)

        val first = searching.continueConversation(context, emptyList(), "mock-search latest news")
        val second = searching.continueConversation(
            context,
            listOf(
                ChatMessage(ChatRole.USER, "mock-search latest news"),
                ChatMessage(ChatRole.MODEL, first)
            ),
            "mock-search latest news"
        )

        assertTrue(first.contains("example web sources"))
        assertTrue(!second.contains("example web sources"))
    }
}
