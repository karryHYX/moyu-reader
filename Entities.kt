package com.moyu.reader.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books", indices = [Index(value = ["fileHash"], unique = true)])
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val format: String,
    val originalFileName: String,
    val privateFilePath: String,
    val fileSize: Long,
    val fileHash: String,
    val charset: String?,
    val addedAt: Long,
    val lastReadAt: Long?,
    val totalCharacters: Long,
    val chapterCount: Int,
    val readingProgress: Float,
    val currentChapter: Int,
    val currentPosition: Int,
    val coverPath: String?,
    val favorite: Boolean,
    val searchIndexed: Boolean,
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId"), Index(value = ["bookId", "chapterIndex"], unique = true)],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    @ColumnInfo(name = "chapterIndex") val index: Int,
    val title: String,
    val locator: String,
    val byteOffset: Long,
    val byteLength: Long,
    val characterCount: Long,
    val synthetic: Boolean,
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId"), Index("chapterId")],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterId: Long,
    val characterOffset: Int,
    val excerpt: String,
    val createdAt: Long,
)

@Entity(
    tableName = "annotations",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId"), Index("chapterId")],
)
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterId: Long,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val note: String,
    val color: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId"), Index("startedAt")],
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val startedAt: Long,
    val endedAt: Long,
    val charactersRead: Long,
)

@Entity(
    tableName = "search_chunks",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("bookId"), Index("chapterId")],
)
data class SearchChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterId: Long,
    val chapterTitle: String,
    val startOffset: Int,
    val content: String,
)

@Fts4
@Entity(tableName = "search_fts")
data class SearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val bookId: String,
    val tokens: String,
)

data class SearchHitRow(
    val chapterId: Long,
    val chapterTitle: String,
    val startOffset: Int,
    val content: String,
)

data class ReadingSummaryRow(
    val totalMillis: Long,
    val sessions: Int,
    val charactersRead: Long,
)
