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
}
