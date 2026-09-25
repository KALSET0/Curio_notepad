package com.curio.notes.data.repository

import com.curio.notes.data.local.dao.NoteDao
import com.curio.notes.data.local.entity.NoteEntity
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(private val noteDao: NoteDao) : NoteRepository {
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
    }

    override suspend fun deleteNotes(ids: Set<Long>) {
        if (ids.isNotEmpty()) noteDao.deleteByIds(ids)
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

    private fun escapeLike(query: String): String =
        query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

}
