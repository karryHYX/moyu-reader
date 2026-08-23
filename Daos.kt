package com.moyu.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY CASE WHEN lastReadAt IS NULL THEN 1 ELSE 0 END, lastReadAt DESC, addedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAllOnce(): List<BookEntity>

    @Query("SELECT * FROM books WHERE fileHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY lastReadAt DESC")
    fun search(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity)

    @Update
    suspend fun update(book: BookEntity)

    @Query("UPDATE books SET currentChapter=:chapter, currentPosition=:position, readingProgress=:progress, lastReadAt=:now WHERE id=:bookId")
    suspend fun updateProgress(bookId: String, chapter: Int, position: Int, progress: Float, now: Long)

    @Query("UPDATE books SET favorite = NOT favorite WHERE id=:bookId")
    suspend fun toggleFavorite(bookId: String)

    @Query("UPDATE books SET title=:title, author=:author WHERE id=:bookId")
    suspend fun updateMetadata(bookId: String, title: String, author: String)

    @Query("UPDATE books SET searchIndexed=:indexed WHERE id=:bookId")
    suspend fun setSearchIndexed(bookId: String, indexed: Boolean)

    @Query("UPDATE books SET currentChapter=:chapter, currentPosition=:position, readingProgress=:progress, lastReadAt=:lastReadAt WHERE id=:bookId")
    suspend fun restoreProgress(bookId: String, chapter: Int, position: Int, progress: Float, lastReadAt: Long?)

    @Query("UPDATE books SET title=:title, author=:author, charset=:charset, totalCharacters=:totalCharacters, chapterCount=:chapterCount, currentChapter=0, currentPosition=0, readingProgress=0, searchIndexed=0 WHERE id=:bookId")
    suspend fun updateAfterReparse(bookId: String, title: String, author: String, charset: String?, totalCharacters: Long, chapterCount: Int)

    @Query("DELETE FROM books WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Set<String>)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE bookId=:bookId ORDER BY chapterIndex")
    fun observeForBook(bookId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId=:bookId ORDER BY chapterIndex")
    suspend fun getForBook(bookId: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id=:id")
    suspend fun getById(id: Long): ChapterEntity?

    @Query("SELECT * FROM chapters ORDER BY bookId, chapterIndex")
    suspend fun getAllOnce(): List<ChapterEntity>

    @Insert
    suspend fun insertAll(chapters: List<ChapterEntity>): List<Long>

    @Query("DELETE FROM chapters WHERE bookId=:bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId=:bookId ORDER BY createdAt DESC")
    fun observeForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Insert
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllOnce(): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmarks WHERE bookId=:bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId=:bookId ORDER BY chapterId, startOffset")
    fun observeForBook(bookId: String): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(annotation: AnnotationEntity): Long

    @Insert
    suspend fun insertAll(annotations: List<AnnotationEntity>)

    @Query("SELECT * FROM annotations")
    suspend fun getAllOnce(): List<AnnotationEntity>

    @Query("DELETE FROM annotations WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM annotations WHERE bookId=:bookId")
    suspend fun deleteForBook(bookId: String)
}

@Dao
interface SearchDao {
    @Insert
    suspend fun insertChunk(chunk: SearchChunkEntity): Long

    @Insert
    suspend fun insertFts(fts: SearchFtsEntity)

    @Transaction
    suspend fun insert(chunk: SearchChunkEntity, tokens: String) {
        val id = insertChunk(chunk)
        insertFts(SearchFtsEntity(id, chunk.bookId, tokens))
    }

    @Query("SELECT c.chapterId, c.chapterTitle, c.startOffset, c.content FROM search_fts f JOIN search_chunks c ON c.id=f.rowid WHERE f.bookId=:bookId AND search_fts MATCH :matchQuery LIMIT :limit")
    suspend fun search(bookId: String, matchQuery: String, limit: Int = 100): List<SearchHitRow>

    @Query("DELETE FROM search_chunks WHERE bookId=:bookId")
    suspend fun deleteChunks(bookId: String)

    @Query("DELETE FROM search_fts WHERE bookId=:bookId")
    suspend fun deleteFts(bookId: String)
}

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSessionEntity)

    @Insert
    suspend fun insertAll(sessions: List<ReadingSessionEntity>)

    @Query("SELECT * FROM reading_sessions")
    suspend fun getAllOnce(): List<ReadingSessionEntity>

    @Query("SELECT COALESCE(SUM(endedAt-startedAt),0) AS totalMillis, COUNT(*) AS sessions, COALESCE(SUM(charactersRead),0) AS charactersRead FROM reading_sessions WHERE startedAt>=:since")
    fun observeSummary(since: Long): Flow<ReadingSummaryRow>
}
