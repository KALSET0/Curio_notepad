package com.curio.notes.ai.providers

import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.ApiKeyCheck
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.AiSource
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
import com.curio.notes.ai.search.GatekeeperDecision
import com.curio.notes.ai.search.WebResult
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.NoteType
import com.curio.notes.testing.FakeWebSearch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
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

    private fun choicesEnvelope(innerJson: String) =
        """{"choices":[{"message":{"role":"assistant","content":$innerJson}}]}"""

    private fun searchingProvider(web: FakeWebSearch) = OpenRouterAIProvider(
        apiKey = "test-key",
        baseUrl = server.url("/").toString(),
        webSearch = web,
        webSearchEnabled = flowOf(true)
    )

    @Test
    fun `gatekeeper approval searches and verifies sources`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(
                    AiJson.encodeToString(AiJson.encodeToString(GatekeeperDecision(true, "coffee")))
                )
            )
        )
        val expected = AIResponse(
            type = NoteType.OTHER,
            title = "t",
            sources = listOf(
                AiSource("Real", "https://real.com/a"),
                AiSource("Invented", "https://fake.example/x")
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(AiJson.encodeToString(AiJson.encodeToString(expected)))
            )
        )
        val web = FakeWebSearch(
            results = listOf(WebResult("Real", "https://real.com/a", "s"))
        )

        val actual = searchingProvider(web).classifyAndAnswer("Is coffee talk true?")

        assertEquals(listOf("coffee"), web.queries)
        assertEquals(listOf(AiSource("Real", "https://real.com/a")), actual.sources)
        server.takeRequest(5, TimeUnit.SECONDS)
        val answerRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(answerRequest)
        assertTrue(answerRequest!!.body.readUtf8().contains("https://real.com/a"))
    }

    @Test
    fun `first conversation turn searches once`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(
                    AiJson.encodeToString(AiJson.encodeToString(GatekeeperDecision(true, "coffee")))
                )
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope("\"Grounded answer.\"")
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope("\"Follow-up answer.\"")
            )
        )
        val web = FakeWebSearch(
            results = listOf(WebResult("Real", "https://real.com/a", "s"))
        )
        val provider = searchingProvider(web)
        val context = ConversationContext("Is coffee true?", null, null)

        val first = provider.continueConversation(context, emptyList(), "Tell me more")
        val second = provider.continueConversation(
            context,
            listOf(
                com.curio.notes.ai.ChatMessage(com.curio.notes.ai.ChatRole.USER, "Tell me more"),
                com.curio.notes.ai.ChatMessage(com.curio.notes.ai.ChatRole.MODEL, first)
            ),
            "And more?"
        )

        assertEquals("Grounded answer.", first)
        assertEquals("Follow-up answer.", second)
        assertEquals(listOf("coffee"), web.queries)
        assertEquals(3, server.requestCount)
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

    @Test
    fun `suggestFollowUps returns parsed questions`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(
                    AiJson.encodeToString(
                        AiJson.encodeToString(mapOf("questions" to listOf("Q1?", "Q2?")))
                    )
                )
            )
        )

        val questions = provider().suggestFollowUps(
            ConversationContext("Original", null, null),
            emptyList()
        )

        assertEquals(listOf("Q1?", "Q2?"), questions)
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertTrue(recorded!!.body.readUtf8().contains("questions"))
    }

    @Test
    fun `validateKey maps server answers`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        assertEquals(ApiKeyCheck.VALID, provider().validateKey("k"))

        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(ApiKeyCheck.INVALID, provider().validateKey("k"))

        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(ApiKeyCheck.UNREACHABLE, provider().validateKey("k"))
    }

    @Test
    fun `validateKey rejects blank without network`() = runTest {
        assertEquals(ApiKeyCheck.INVALID, provider().validateKey("  "))
        assertEquals(0, server.requestCount)
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
