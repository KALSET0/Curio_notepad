package com.curio.notes.ai.providers

import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.AiSource
import com.curio.notes.ai.search.GatekeeperDecision
import com.curio.notes.ai.search.WebResult
import com.curio.notes.domain.model.NoteType
import com.curio.notes.testing.FakeWebSearch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OllamaAIProviderTest {
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

    private fun provider(
        url: String = server.url("/").toString(),
        model: String = "qwen3:8b",
        webSearch: FakeWebSearch? = null
    ) = OllamaAIProvider(
        serverUrl = flowOf(url),
        modelName = flowOf(model),
        webSearch = webSearch,
        webSearchEnabled = flowOf(webSearch != null)
    )

    private fun choicesEnvelope(innerJson: String) =
        """{"choices":[{"message":{"role":"assistant","content":$innerJson}}]}"""

    @Test
    fun `success parses structured json without auth`() = runTest {
        val expected = AIResponse(
            type = NoteType.QUESTION,
            title = "Quantum?",
            summary = "Short answer."
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(AiJson.encodeToString(AiJson.encodeToString(expected)))
            )
        )

        val actual = provider().classifyAndAnswer("What is X?")

        assertEquals(expected, actual)
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertTrue(recorded!!.path!!.contains("chat/completions"))
        assertNull(recorded.getHeader("Authorization"))
        val requestBody = recorded.body.readUtf8()
        assertTrue(requestBody.contains("qwen3:8b"))
        assertTrue(requestBody.contains("response_format"))
        assertTrue(requestBody.contains("sources"))
    }

    @Test
    fun `blank url fails as not configured without network`() = runTest {
        try {
            provider(url = "  ").classifyAndAnswer("Hello?")
            fail("Expected OllamaNotConfigured")
        } catch (e: AiException.OllamaNotConfigured) {
            assertEquals("ollama_not_configured", e.code)
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `normalize handles user input`() {
        assertEquals("", normalizeOllamaBaseUrl(""))
        assertEquals("", normalizeOllamaBaseUrl("   "))
        assertEquals(
            "http://192.168.1.5:11434/v1",
            normalizeOllamaBaseUrl("192.168.1.5:11434")
        )
        assertEquals(
            "http://192.168.1.5:11434/v1",
            normalizeOllamaBaseUrl("http://192.168.1.5:11434/")
        )
        assertEquals(
            "http://192.168.1.5:11434/v1",
            normalizeOllamaBaseUrl("http://192.168.1.5:11434/v1")
        )
        assertEquals("https://myhost/v1", normalizeOllamaBaseUrl("https://myhost"))
        assertEquals("", normalizeOllamaBaseUrl("ftp://myhost"))
        assertEquals("", normalizeOllamaBaseUrl("http://"))
    }

    @Test
    fun `test connection hits api tags`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"models":[]}"""))

        assertTrue(provider().testConnection())

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertTrue(recorded!!.path!!.contains("/api/tags"))
    }

    @Test
    fun `test connection fails closed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        assertFalse(provider().testConnection())
        assertFalse(provider(url = "").testConnection())
    }

    @Test
    fun `gatekeeper approval searches and verifies sources`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(
                    AiJson.encodeToString(
                        AiJson.encodeToString(GatekeeperDecision(true, "coffee"))
                    )
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

        val actual = provider(webSearch = web).classifyAndAnswer("Is coffee talk true?")

        assertEquals(listOf("coffee"), web.queries)
        assertEquals(listOf(AiSource("Real", "https://real.com/a")), actual.sources)
    }

    @Test
    fun `conversation extracts message content`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(AiJson.encodeToString("Just the answer."))
            )
        )

        val reply = provider().continueConversation(
            com.curio.notes.ai.ConversationContext("Original", null, null),
            emptyList(),
            "Tell me more"
        )

        assertEquals("Just the answer.", reply)
    }

    @Test
    fun `think traces are stripped from chat replies`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(
                    AiJson.encodeToString(
                        "<think>I should mention everything</think>Real answer here."
                    )
                )
            )
        )

        val reply = provider().continueConversation(
            com.curio.notes.ai.ConversationContext("Original", null, null),
            emptyList(),
            "Tell me more"
        )

        assertEquals("Real answer here.", reply)
    }

    @Test
    fun `only thinking left fails as invalid response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                choicesEnvelope(AiJson.encodeToString("<think>hmm</think>"))
            )
        )

        try {
            provider().continueConversation(
                com.curio.notes.ai.ConversationContext("Original", null, null),
                emptyList(),
                "Tell me more"
            )
            fail("Expected InvalidResponse")
        } catch (e: AiException.InvalidResponse) {
            // expected: nothing usable remains
        }
    }

    @Test
    fun `stripThinkBlocks keeps untagged text`() {
        assertEquals("plain", stripThinkBlocks("plain"))
        assertEquals("a  b", stripThinkBlocks("a <THINK>x\ny</THINK> b"))
        assertEquals("<think>oops", stripThinkBlocks("<think>oops"))
    }
}
