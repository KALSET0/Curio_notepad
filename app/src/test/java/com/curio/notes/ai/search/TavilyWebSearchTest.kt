package com.curio.notes.ai.search

import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiLanguage
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class TavilyWebSearchTest {
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

    private fun search(apiKey: String = "test-key") = TavilyWebSearch(
        apiKey = apiKey,
        baseUrl = server.url("/").toString()
    )

    @Test
    fun `success parses results and trims snippets`() = runTest {
        val longContent = "x".repeat(500)
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"results":[
                    {"title":"T1","url":"https://a.com/1","content":"short"},
                    {"title":"","url":"https://a.com/2","content":"$longContent"},
                    {"title":"NoUrl","url":"","content":"skip me"}
                ]}"""
            )
        )

        val results = search().search("coffee", 5, AiLanguage.ENGLISH)

        assertEquals(2, results.size)
        assertEquals(WebResult("T1", "https://a.com/1", "short"), results[0])
        assertEquals("https://a.com/2", results[1].url)
        assertEquals("https://a.com/2", results[1].title)
        assertEquals(TavilyConfig.MAX_SNIPPET_CHARS, results[1].snippet.length)

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)!!
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("coffee"))
        assertTrue(body.contains("test-key"))
        assertTrue(recorded.path!!.contains("/search"))
    }

    @Test
    fun `blank key fails fast without network`() = runTest {
        try {
            search(apiKey = "").search("coffee", 5, AiLanguage.ENGLISH)
            fail("Expected MissingApiKey")
        } catch (e: AiException.MissingApiKey) {
            // expected
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `unauthorized maps to invalid key`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        try {
            search().search("coffee", 5, AiLanguage.ENGLISH)
            fail("Expected InvalidApiKey")
        } catch (e: AiException.InvalidApiKey) {
            // expected
        }
    }

    @Test
    fun `server error maps to service error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        try {
            search().search("coffee", 5, AiLanguage.ENGLISH)
            fail("Expected ServiceError")
        } catch (e: AiException.ServiceError) {
            // expected
        }
    }

    @Test
    fun `empty results parse to empty list`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"results":[]}"""))

        assertTrue(search().search("coffee", 5, AiLanguage.ENGLISH).isEmpty())
    }
}
