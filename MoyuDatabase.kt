package com.moyu.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        AnnotationEntity::class,
        ReadingSessionEntity::class,
        SearchChunkEntity::class,
        SearchFtsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MoyuDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun searchDao(): SearchDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        fun create(context: Context): MoyuDatabase = Room.databaseBuilder(
            context.applicationContext,
            MoyuDatabase::class.java,
            "moyu-reader.db",
        ).setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }
}

