package com.curio.notes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalText: String,
    // Enum names as plain strings; null until AI classifies the note (Phase 4+).
    val type: String?,
    val aiResponseJson: String?,
    val errorMessage: String?,
    val status: String,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    // Developer-mode diagnostics from the last generation (provider label
    // like "Ollama · qwen3:8b" and end-to-end millis). Null when unknown.
    val generationLabel: String? = null,
    val generationMillis: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
