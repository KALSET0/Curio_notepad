package com.curio.notes.ai

import com.curio.notes.ai.search.WebResult
import com.curio.notes.domain.model.NoteType
import kotlinx.serialization.Serializable

@Serializable
data class AiSource(
    val title: String,
    val url: String
)

@Serializable
data class AIResponse(
    val type: NoteType,
    val title: String,
    val summary: String? = null,
    val explanation: String? = null,
    val examples: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val relatedTopics: List<String> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    // Sources the model claims to have used, verified against delivered URLs.
    val sources: List<AiSource> = emptyList()
)

/** Keeps only sources whose URL was actually delivered (anti-hallucination). */
fun AIResponse.withVerifiedSources(delivered: List<WebResult>): AIResponse {
    if (sources.isEmpty() || delivered.isEmpty()) return copy(sources = emptyList())
    val urls = delivered.map { it.url }.toSet()
    return copy(sources = sources.filter { it.url in urls })
}
