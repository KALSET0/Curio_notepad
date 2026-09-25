package com.curio.notes.ai

import com.curio.notes.ai.providers.MockAIProvider
import com.curio.notes.domain.model.NoteType
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
}
