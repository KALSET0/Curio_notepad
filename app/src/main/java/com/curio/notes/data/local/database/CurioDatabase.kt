package com.curio.notes.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.curio.notes.data.local.dao.NoteDao
import com.curio.notes.data.local.entity.NoteEntity

@Database(entities = [NoteEntity::class], version = 3, exportSchema = false)
abstract class CurioDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
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
