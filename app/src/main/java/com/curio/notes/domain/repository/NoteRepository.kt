package com.curio.notes.domain.repository

import com.curio.notes.ai.ChatMessage
import com.curio.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeArchivedNotes(): Flow<List<Note>>
    fun observeNote(noteId: Long): Flow<Note?>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun createNote(title: String, originalText: String): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(noteId: Long)
    suspend fun deleteNotes(ids: Set<Long>)
    suspend fun setArchived(ids: Set<Long>, archived: Boolean)
    suspend fun setPinned(ids: Set<Long>, pinned: Boolean)
    suspend fun getPendingNoteIds(): List<Long>
    suspend fun resetStuckProcessingNotes()
    fun observeConversation(noteId: Long): Flow<List<ChatMessage>>
    suspend fun appendConversationMessage(noteId: Long, message: ChatMessage)
    suspend fun clearConversation(noteId: Long)
}
