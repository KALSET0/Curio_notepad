package com.curio.notes.ai.providers

import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.NoteType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OpenRouterAIProviderTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun provider(apiKey: String = "test-key") = OpenRouterAIProvider(
        apiKey = apiKey,
        baseUrl = server.url("/").toString()
    )

    @Test
    fun `success parses structured json and sends schema`() = runTest {
        val expected = AIResponse(
            type = NoteType.QUESTION,
            title = "Quantum?",
            summary = "Short answer.",
            explanation = "Longer explanation.",
            examples = listOf("ex"),
            keyPoints = listOf("kp"),
            relatedTopics = listOf("rt"),
            followUpQuestions = listOf("fq")
        )
        val envelope = """{"choices":[{"message":{"role":"assistant","content":${
            AiJson.encodeToString(AiJson.encodeToString(expected))
        }}}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(envelope))

        val actual = provider().classifyAndAnswer("What is X?")

        assertEquals(expected, actual)
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertTrue(recorded!!.path!!.contains("chat/completions"))
        assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
        val requestBody = recorded.body.readUtf8()
        assertTrue(requestBody.contains("openrouter/free"))
        assertTrue(requestBody.contains("response_format"))
        assertTrue(requestBody.contains("What is X?"))
    }

    @Test
    fun `blank key fails fast without network`() = runTest {
        failsAs(AiException.MissingApiKey::class.java) {
            provider(apiKey = "").classifyAndAnswer("Hello?")
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `spanish language sends spanish system prompt`() = runTest {
        val expected = AIResponse(type = NoteType.OTHER, title = "t")
        val envelope = """{"choices":[{"message":{"role":"assistant","content":${
            AiJson.encodeToString(AiJson.encodeToString(expected))
        }}}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(envelope))

        val spanish = OpenRouterAIProvider(
            apiKey = "test-key",
            baseUrl = server.url("/").toString(),
            language = flowOf(AppLanguage.SPANISH)
        )
        spanish.classifyAndAnswer("¿Qué es X?")

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        val requestBody = recorded!!.body.readUtf8()
        assertTrue(requestBody.contains("espa"))
        assertTrue(!requestBody.contains("Guiding principle"))
    }

    @Test
    fun `rate limit maps to friendly error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("{}"))
        failsAs(AiException.RateLimited::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `unauthorized maps to invalid key`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        failsAs(AiException.InvalidApiKey::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `garbage payload maps to invalid response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json{{{"))
        failsAs(AiException.InvalidResponse::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `empty choices map to invalid response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[]}"""))
        failsAs(AiException.InvalidResponse::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `conversation returns prose without schema`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"choices":[{"message":{"role":"assistant","content":"Plants convert light."}}]}"""
            )
        )
        val context = ConversationContext(
            originalText = "Define photosynthesis",
            type = NoteType.CONCEPT,
            summary = "Plants make food.",
            relatedTopics = listOf("Chlorophyll")
        )
        val history = listOf(
            ChatMessage(ChatRole.USER, "Hi"),
            ChatMessage(ChatRole.MODEL, "Hello!")
        )

        val reply = provider().continueConversation(context, history, "Tell me more")

        assertEquals("Plants convert light.", reply)
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        val body = recorded!!.body.readUtf8()
        assertTrue(!body.contains("response_format"))
        assertTrue(body.contains("Tell me more"))
        assertTrue(body.contains("Hi"))
        assertTrue(body.contains("Chlorophyll"))
    }

    private suspend fun failsAs(expected: Class<out AiException>, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected ${expected.simpleName}")
        } catch (e: AiException) {
            assertTrue(
                "Expected ${expected.simpleName} but was ${e::class.simpleName}",
                expected.isInstance(e)
            )
        }
    }
}
