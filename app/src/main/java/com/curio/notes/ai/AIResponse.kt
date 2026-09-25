package com.curio.notes.ai

import com.curio.notes.domain.model.NoteType
import kotlinx.serialization.Serializable

@Serializable
data class AIResponse(
    val type: NoteType,
    val title: String,
    val summary: String? = null,
    val explanation: String? = null,
    val examples: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val relatedTopics: List<String> = emptyList(),
    val followUpQuestions: List<String> = emptyList()
)
