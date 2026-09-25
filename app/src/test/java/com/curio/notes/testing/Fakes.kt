package com.curio.notes.testing

import com.curio.notes.ai.ChatMessage
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNoteRepository : NoteRepository {
    private val notes = MutableStateFlow(mapOf<Long, Note>())
    private val conversations = MutableStateFlow(mapOf<Long, List<ChatMessage>>())
    private var nextId = 1L

    override fun observeNotes(): Flow<List<Note>> =
        notes.map { all ->
            all.values
                .filter { !it.isArchived }
                .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt })
        }

    override fun observeArchivedNotes(): Flow<List<Note>> =
        notes.map { all ->
            all.values
                .filter { it.isArchived }
                .sortedByDescending { it.createdAt }
        }

    override fun observeNote(noteId: Long): Flow<Note?> =
        notes.map { it[noteId] }

    override fun searchNotes(query: String): Flow<List<Note>> =
        notes.map { map ->
            map.values.filter { note ->
                !note.isArchived && (
                    note.title.contains(query, ignoreCase = true) ||
                        note.originalText.contains(query, ignoreCase = true) ||
                        (note.aiResponseJson?.contains(query, ignoreCase = true) == true)
                )
            }
        }

    override suspend fun createNote(title: String, originalText: String): Long {
        val id = nextId++
        val now = System.currentTimeMillis()
        notes.value += id to Note(
            id = id,
            title = title,
            originalText = originalText,
            createdAt = now,
            updatedAt = now
        )
        return id
    }

    override suspend fun updateNote(note: Note) {
        notes.value += note.id to note
    }

    override suspend fun deleteNote(noteId: Long) {
        notes.value -= noteId
        conversations.value -= noteId
    }

    override suspend fun deleteNotes(ids: Set<Long>) {
        notes.value -= ids
        conversations.value -= ids
    }

    override suspend fun setArchived(ids: Set<Long>, archived: Boolean) {
        notes.value = notes.value.mapValues { (id, note) ->
            if (id in ids) note.copy(isArchived = archived) else note
        }
    }

    override suspend fun setPinned(ids: Set<Long>, pinned: Boolean) {
        notes.value = notes.value.mapValues { (id, note) ->
            if (id in ids) note.copy(isPinned = pinned) else note
        }
    }

    override suspend fun getPendingNoteIds(): List<Long> =
        notes.value.values
            .filter { it.status == NoteStatus.PENDING }
            .map { it.id }

    override suspend fun resetStuckProcessingNotes() {
        notes.value = notes.value.mapValues { (_, note) ->
            if (note.status == NoteStatus.PROCESSING) {
                note.copy(status = NoteStatus.PENDING, errorMessage = null)
            } else {
                note
            }
        }
    }

    override fun observeConversation(noteId: Long): Flow<List<ChatMessage>> =
        conversations.map { it[noteId].orEmpty() }

    override suspend fun appendConversationMessage(noteId: Long, message: ChatMessage) {
        conversations.value += noteId to (conversations.value[noteId].orEmpty() + message)
    }

    override suspend fun clearConversation(noteId: Long) {
        conversations.value -= noteId
    }
}
