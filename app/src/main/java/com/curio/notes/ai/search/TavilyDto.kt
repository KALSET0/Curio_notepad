package com.curio.notes.ai.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TavilySearchRequest(
    @SerialName("api_key") val apiKey: String,
    val query: String,
    @SerialName("max_results") val maxResults: Int,
    @SerialName("search_depth") val searchDepth: String = "basic",
    @SerialName("include_answer") val includeAnswer: Boolean = false
)

@Serializable
internal data class TavilySearchResponse(
    val results: List<TavilyResult> = emptyList()
)

@Serializable
internal data class TavilyResult(
    val title: String = "",
    val url: String = "",
    val content: String = ""
)
