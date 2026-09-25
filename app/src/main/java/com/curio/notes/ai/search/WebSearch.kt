package com.curio.notes.ai.search

import com.curio.notes.ai.AiLanguage

/** A single web result: title, exact URL, and a short snippet. */
data class WebResult(
    val title: String,
    val url: String,
    val snippet: String
)

interface WebSearchProvider {
    suspend fun search(query: String, maxResults: Int, language: AiLanguage): List<WebResult>
}
