package com.curio.notes.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.curio.notes.data.local.dao.ConversationDao
import com.curio.notes.data.local.dao.NoteDao
import com.curio.notes.data.local.entity.ConversationMessageEntity
import com.curio.notes.data.local.entity.NoteEntity

@Database(
    entities = [NoteEntity::class, ConversationMessageEntity::class],
    version = 5,
    exportSchema = false
)
abstract class CurioDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun conversationDao(): ConversationDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN errorMessage TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS conversation_messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "noteId INTEGER NOT NULL, " +
                "role TEXT NOT NULL, " +
                "text TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL, " +
                "FOREIGN KEY(noteId) REFERENCES notes(id) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_conversation_messages_noteId " +
                "ON conversation_messages(noteId)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN generationLabel TEXT")
        db.execSQL("ALTER TABLE notes ADD COLUMN generationMillis INTEGER")
        db.execSQL("ALTER TABLE conversation_messages ADD COLUMN generationLabel TEXT")
        db.execSQL("ALTER TABLE conversation_messages ADD COLUMN generationMillis INTEGER")
    }
}
