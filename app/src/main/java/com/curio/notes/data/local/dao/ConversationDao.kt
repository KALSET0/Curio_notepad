package com.curio.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.curio.notes.data.local.entity.ConversationMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query(
        "SELECT * FROM conversation_messages WHERE noteId = :noteId " +
            "ORDER BY createdAt ASC, id ASC"
    )
    fun observeMessages(noteId: Long): Flow<List<ConversationMessageEntity>>

    @Insert
    suspend fun insert(message: ConversationMessageEntity): Long

    @Query("DELETE FROM conversation_messages WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    // Keeps the first keepCount messages (chronological) and drops the rest.
    // keepCount = 0 clears everything, mirroring clearConversation callers.
    @Query(
        "DELETE FROM conversation_messages WHERE noteId = :noteId AND id NOT IN (" +
            "SELECT id FROM conversation_messages WHERE noteId = :noteId " +
            "ORDER BY createdAt ASC, id ASC LIMIT :keepCount)"
    )
    suspend fun truncateToCount(noteId: Long, keepCount: Int)
}
