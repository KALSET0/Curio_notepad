package com.curio.notes.ai.providers

import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.ApiKeyCheck
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
import okhttp3.OkHttpClient
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

class GeminiAIProviderTest {
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

    private fun provider(apiKey: String = "test-key") = GeminiAIProvider(
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
        val envelope = """{"candidates":[{"content":{"parts":[{"text":${
            AiJson.encodeToString(AiJson.encodeToString(expected))
        }}]},"finishReason":"STOP"}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(envelope))

        val actual = provider().classifyAndAnswer("What is X?")

        assertEquals(expected, actual)
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertTrue(recorded!!.path!!.contains("gemini-3.8-flash:generateContent"))
        assertEquals("test-key", recorded.getHeader("x-goog-api-key"))
        val requestBody = recorded.body.readUtf8()
        assertTrue(requestBody.contains("responseSchema"))
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
        val envelope = """{"candidates":[{"content":{"parts":[{"text":${
            AiJson.encodeToString(AiJson.encodeToString(expected))
        }}]},"finishReason":"STOP"}]}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(envelope))

        val spanish = GeminiAIProvider(
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

    private fun geminiEnvelope(innerJson: String) =
        """{"candidates":[{"content":{"parts":[{"text":$innerJson}]},"finishReason":"STOP"}]}"""

    private fun searchingProvider(web: FakeWebSearch) = GeminiAIProvider(
        apiKey = "test-key",
        baseUrl = server.url("/").toString(),
        webSearch = web,
        webSearchEnabled = flowOf(true)
    )

    @Test
    fun `gatekeeper approval searches and verifies sources`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                geminiEnvelope(
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
                geminiEnvelope(AiJson.encodeToString(AiJson.encodeToString(expected)))
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
        val answerBody = answerRequest!!.body.readUtf8()
        assertTrue(answerBody.contains("https://real.com/a"))
        assertTrue(answerBody.contains("sources"))
    }

    @Test
    fun `gatekeeper denial skips search`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                geminiEnvelope(
                    AiJson.encodeToString(AiJson.encodeToString(GatekeeperDecision(false, "")))
                )
            )
        )
        val expected = AIResponse(type = NoteType.OTHER, title = "t")
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                geminiEnvelope(AiJson.encodeToString(AiJson.encodeToString(expected)))
            )
        )
        val web = FakeWebSearch(
            results = listOf(WebResult("Real", "https://real.com/a", "s"))
        )

        val actual = searchingProvider(web).classifyAndAnswer("Buy milk tomorrow")

        assertTrue(web.queries.isEmpty())
        assertTrue(actual.sources.isEmpty())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `disabled search makes a single call`() = runTest {
        val expected = AIResponse(type = NoteType.OTHER, title = "t")
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                geminiEnvelope(AiJson.encodeToString(AiJson.encodeToString(expected)))
            )
        )
        val web = FakeWebSearch(
            results = listOf(WebResult("Real", "https://real.com/a", "s"))
        )
        val provider = GeminiAIProvider(
            apiKey = "test-key",
            baseUrl = server.url("/").toString(),
            webSearch = web,
            webSearchEnabled = flowOf(false)
        )

        provider.classifyAndAnswer("Is coffee talk true?")

        assertTrue(web.queries.isEmpty())
        assertEquals(1, server.requestCount)
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
    fun `empty candidates map to invalid response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"candidates":[]}"""))
        failsAs(AiException.InvalidResponse::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `conversation returns prose without schema`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"candidates":[{"content":{"parts":[{"text":"Plants convert light."}]},"finishReason":"STOP"}]}"""
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
        assertTrue(!body.contains("responseSchema"))
        assertTrue(body.contains("Tell me more"))
        assertTrue(body.contains("Hi"))
        assertTrue(body.contains("Chlorophyll"))
    }

    @Test
    fun `server error maps`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        failsAs(AiException.ServiceError::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `empty object maps to invalid response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"candidates":[{"content":{"parts":[{"text":"{}"}]},"finishReason":"STOP"}]}"""
            )
        )
        failsAs(AiException.InvalidResponse::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `unknown type maps to invalid response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"candidates":[{"content":{"parts":[{"text":"{\"type\":\"NOPE\",\"title\":\"t\"}"}]},"finishReason":"STOP"}]}"""
            )
        )
        failsAs(AiException.InvalidResponse::class.java) {
            provider().classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `connection refused maps to network`() = runTest {
        val dead = MockWebServer()
        dead.start()
        val url = dead.url("/").toString()
        dead.shutdown()
        failsAs(AiException.Network::class.java) {
            GeminiAIProvider(apiKey = "k", baseUrl = url).classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `silent server maps to timeout`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val impatient = GeminiAIProvider(
            apiKey = "k",
            baseUrl = server.url("/").toString(),
            client = OkHttpClient.Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build()
        )
        failsAs(AiException.TimedOut::class.java) {
            impatient.classifyAndAnswer("Hello?")
        }
    }

    @Test
    fun `suggestFollowUps returns parsed questions`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                geminiEnvelope(
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
        val requestBody = recorded!!.body.readUtf8()
        assertTrue(requestBody.contains("follow-up"))
        assertTrue(requestBody.contains("questions"))
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
