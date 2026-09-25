package com.curio.notes.ai.search

import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class GatekeeperTest {

    @Test
    fun `valid decision parses`() {
        val decision = parseGatekeeperDecision("""{"need_search":true,"query":"coffee growth"}""")

        assertTrue(decision.need_search)
        assertEquals("coffee growth", decision.query)
    }

    @Test
    fun `malformed output degrades to no search`() {
        assertEquals(GatekeeperDecision(), parseGatekeeperDecision("not json"))
        assertEquals(GatekeeperDecision(), parseGatekeeperDecision(null))
        assertEquals(GatekeeperDecision(), parseGatekeeperDecision("{}"))
    }

    @Test
    fun `search runs for approved queries`() = runTest {
        val fake = FakeWebSearch(results = listOf(WebResult("T", "https://u", "s")))

        val results = fake.executeSearch("coffee", 5, AiLanguage.ENGLISH)

        assertEquals(1, results.size)
        assertEquals(listOf("coffee"), fake.queries)
    }

    @Test
    fun `blank query never hits the network`() = runTest {
        val fake = FakeWebSearch()

        assertTrue(fake.executeSearch("  ", 5, AiLanguage.ENGLISH).isEmpty())
        assertTrue(fake.queries.isEmpty())
    }

    @Test
    fun `missing key surfaces search failure`() = runTest {
        val fake = FakeWebSearch(failure = AiException.MissingApiKey())

        try {
            fake.executeSearch("coffee", 5, AiLanguage.ENGLISH)
            fail("Expected SearchFailed")
        } catch (e: AiException.SearchFailed) {
            assertEquals("search_failed", e.code)
        }
    }

    @Test
    fun `rejected key surfaces search failure`() = runTest {
        val fake = FakeWebSearch(failure = AiException.InvalidApiKey())

        try {
            fake.executeSearch("coffee", 5, AiLanguage.ENGLISH)
            fail("Expected SearchFailed")
        } catch (e: AiException.SearchFailed) {
            // expected: user opted in but the key is wrong
        }
    }

    @Test
    fun `network trouble degrades to no sources`() = runTest {
        val fake = FakeWebSearch(failure = AiException.Network(IOException("down")))

        assertTrue(fake.executeSearch("coffee", 5, AiLanguage.ENGLISH).isEmpty())
    }

    @Test
    fun `decision defaults mean no search`() {
        assertFalse(GatekeeperDecision().need_search)
        assertTrue(GatekeeperDecision().query.isEmpty())
    }

    private class FakeWebSearch(
        var results: List<WebResult> = emptyList(),
        var failure: IOException? = null,
        val queries: MutableList<String> = mutableListOf()
    ) : WebSearchProvider {
        override suspend fun search(
            query: String,
            maxResults: Int,
            language: AiLanguage
        ): List<WebResult> {
            queries += query
            failure?.let { throw it }
            return results
        }
    }
}
