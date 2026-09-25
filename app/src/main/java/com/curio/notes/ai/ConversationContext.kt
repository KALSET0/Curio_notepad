package com.curio.notes.ai

import com.curio.notes.domain.model.NoteType

data class ConversationContext(
    val originalText: String,
    val type: NoteType?,
    val summary: String?,
    val relatedTopics: List<String> = emptyList(),
    // Already-proposed follow-ups, so refreshes do not repeat them.
    val followUpQuestions: List<String> = emptyList()
)
