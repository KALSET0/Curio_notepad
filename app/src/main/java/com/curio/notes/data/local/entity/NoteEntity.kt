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
    val createdAt: Long,
    val updatedAt: Long
)
