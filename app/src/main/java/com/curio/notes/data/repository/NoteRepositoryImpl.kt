package com.curio.notes.data.repository

import com.curio.notes.data.local.dao.ConversationDao
import com.curio.notes.data.local.dao.NoteDao
import com.curio.notes.data.local.entity.ConversationMessageEntity
import com.curio.notes.data.local.entity.NoteEntity
import com.curio.notes.ai.ChatMessage
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val conversationDao: ConversationDao
) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeNotes().map { entities -> entities.map { it.toDomain() } }

    override fun observeArchivedNotes(): Flow<List<Note>> =
        noteDao.observeArchivedNotes().map { entities -> entities.map { it.toDomain() } }

    override fun observeNote(noteId: Long): Flow<Note?> =
        noteDao.observeNoteById(noteId).map { it?.toDomain() }

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes(escapeLike(query)).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createNote(title: String, originalText: String): Long {
        val now = System.currentTimeMillis()
        return noteDao.insert(
            NoteEntity(
                title = title.ifBlank { deriveTitle(originalText) },
                originalText = originalText,
                type = null,
                aiResponseJson = null,
                status = NoteStatus.PENDING.name,
                errorMessage = null,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateNote(note: Note) {
        noteDao.update(note.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun deleteNote(noteId: Long) {
        noteDao.deleteById(noteId)
        conversationDao.deleteByNoteId(noteId)
    }

    override suspend fun deleteNotes(ids: Set<Long>) {
        if (ids.isNotEmpty()) noteDao.deleteByIds(ids)
        ids.forEach { conversationDao.deleteByNoteId(it) }
    }

    override suspend fun setArchived(ids: Set<Long>, archived: Boolean) {
        if (ids.isNotEmpty()) noteDao.setArchived(ids, archived)
    }

    override suspend fun setPinned(ids: Set<Long>, pinned: Boolean) {
        if (ids.isNotEmpty()) noteDao.setPinned(ids, pinned)
    }

    override suspend fun getPendingNoteIds(): List<Long> =
        noteDao.getNoteIdsByStatus(NoteStatus.PENDING.name)

    override suspend fun resetStuckProcessingNotes() {
        noteDao.resetStuckProcessingNotes()
    }

    override fun observeConversation(noteId: Long): Flow<List<ChatMessage>> =
        conversationDao.observeMessages(noteId).map { entities ->
            entities.map { it.toChatMessage() }
        }

    override suspend fun appendConversationMessage(noteId: Long, message: ChatMessage) {
        conversationDao.insert(
            ConversationMessageEntity(
                noteId = noteId,
                role = message.role.name,
                text = message.text,
                generationLabel = message.generationLabel,
                generationMillis = message.generationMillis,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearConversation(noteId: Long) {
        conversationDao.deleteByNoteId(noteId)
    }

    private fun escapeLike(query: String): String =
        query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

}
