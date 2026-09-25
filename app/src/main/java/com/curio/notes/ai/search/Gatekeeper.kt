package com.curio.notes.ai.search

import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.AiLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/** The gatekeeper's verdict. Defaults mean "no search" so malformed
 * output degrades gracefully instead of triggering stray searches. */
@Serializable
data class GatekeeperDecision(
    val need_search: Boolean = false,
    val query: String = ""
)

fun parseGatekeeperDecision(json: String?): GatekeeperDecision =
    json?.let {
        runCatching { AiJson.decodeFromString<GatekeeperDecision>(it) }.getOrNull()
    } ?: GatekeeperDecision()

/**
 * Runs the web search for a gatekeeper-approved query.
 * Key problems (missing/rejected key) surface as [AiException.SearchFailed]
 * because the user explicitly opted in; anything else degrades to no sources.
 */
suspend fun WebSearchProvider.executeSearch(
    query: String,
    maxResults: Int,
    language: AiLanguage
): List<WebResult> {
    if (query.isBlank()) return emptyList()
    return try {
        search(query, maxResults, language)
    } catch (e: CancellationException) {
        throw e
    } catch (e: AiException.MissingApiKey) {
        throw AiException.SearchFailed()
    } catch (e: AiException.InvalidApiKey) {
        throw AiException.SearchFailed()
    } catch (e: Exception) {
        emptyList()
    }
}
