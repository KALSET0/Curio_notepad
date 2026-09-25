package com.curio.notes.data.repository

import com.curio.notes.data.local.entity.NoteEntity
import com.curio.notes.data.local.entity.ConversationMessageEntity
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.model.NoteType

internal fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    originalText = originalText,
    type = type?.let { runCatching { NoteType.valueOf(it) }.getOrNull() },
    aiResponseJson = aiResponseJson,
    errorMessage = errorMessage,
    status = runCatching { NoteStatus.valueOf(status) }.getOrDefault(NoteStatus.PENDING),
    isArchived = isArchived,
    isPinned = isPinned,
    generationLabel = generationLabel,
    generationMillis = generationMillis,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    originalText = originalText,
    type = type?.name,
    aiResponseJson = aiResponseJson,
    errorMessage = errorMessage,
    status = status.name,
    isArchived = isArchived,
    isPinned = isPinned,
    generationLabel = generationLabel,
    generationMillis = generationMillis,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun ConversationMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    role = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.USER),
    text = text,
    generationLabel = generationLabel,
    generationMillis = generationMillis
)
