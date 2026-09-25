package com.curio.notes.domain.model

data class Note(
    val id: Long = 0,
    val title: String,
    val originalText: String,
    val type: NoteType? = null,
    // Raw AI payload, decoded to AIResponse where rendered.
    val aiResponseJson: String? = null,
    // Last user-facing processing failure, if any. Cleared on retry/success.
    val errorMessage: String? = null,
    val status: NoteStatus = NoteStatus.PENDING,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
