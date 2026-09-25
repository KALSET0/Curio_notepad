package com.curio.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.curio.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun observeNoteById(noteId: Long): Flow<NoteEntity?>

    // aiResponseJson holds the whole AI payload, so one LIKE covers
    // AI-generated content and related topics (both live inside that JSON).
    // User-typed %, _ and \ are escaped by callers; ESCAPE '\' keeps them literal.
    @Query(
        "SELECT * FROM notes WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' ESCAPE '\\' " +
            "OR originalText LIKE '%' || :query || '%' ESCAPE '\\' " +
            "OR aiResponseJson LIKE '%' || :query || '%' ESCAPE '\\') ORDER BY createdAt DESC"
    )
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Set<Long>)

    @Query("UPDATE notes SET isArchived = :archived WHERE id IN (:ids)")
    suspend fun setArchived(ids: Set<Long>, archived: Boolean)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id IN (:ids)")
    suspend fun setPinned(ids: Set<Long>, pinned: Boolean)

    @Query("SELECT id FROM notes WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getNoteIdsByStatus(status: String): List<Long>

    @Query("UPDATE notes SET status = 'PENDING', errorMessage = NULL WHERE status = 'PROCESSING'")
    suspend fun resetStuckProcessingNotes()
}
