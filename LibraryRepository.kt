package com.moyu.reader.data

import android.content.Context
import androidx.room.withTransaction
import com.moyu.reader.data.db.BookDao
import com.moyu.reader.data.db.ChapterDao
import com.moyu.reader.data.db.ChapterEntity
import com.moyu.reader.data.db.BookmarkEntity
import com.moyu.reader.data.db.MoyuDatabase
import com.moyu.reader.data.db.ReadingSessionEntity
import com.moyu.reader.data.db.SearchChunkEntity
import com.moyu.reader.data.db.asExternalModel
import com.moyu.reader.model.Book
import com.moyu.reader.model.BookFormat
import com.moyu.reader.model.Bookmark
import com.moyu.reader.model.Chapter
import com.moyu.reader.model.ReaderAnchor
import com.moyu.reader.model.ReadingSummary
import com.moyu.reader.model.SearchHit
import com.moyu.reader.parser.ParsedChapter
import com.moyu.reader.parser.ParseRequest
import com.moyu.reader.parser.ParserRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedHashMap

class LibraryRepository(
    private val context: Context,
    private val database: MoyuDatabase,
    private val parsers: ParserRegistry,
) {
    private val books: BookDao = database.bookDao()
    private val chapters: ChapterDao = database.chapterDao()
    private val cache = ChapterCache(maxCharacters = 3_000_000)

    fun observeBooks(): Flow<List<Book>> = books.observeAll().map { list -> list.map { it.asExternalModel() } }
    fun observeBook(id: String): Flow<Book?> = books.observeById(id).map { it?.asExternalModel() }
    fun observeChapters(bookId: String): Flow<List<Chapter>> = chapters.observeForBook(bookId).map { list -> list.map { it.asExternalModel() } }
    fun searchLibrary(query: String): Flow<List<Book>> = books.search(query.trim()).map { list -> list.map { it.asExternalModel() } }
    fun observeBookmarks(bookId: String): Flow<List<Bookmark>> = database.bookmarkDao().observeForBook(bookId).map { list ->
        list.map { Bookmark(it.id, it.bookId, it.chapterId, it.characterOffset, it.excerpt, it.createdAt) }
    }
    fun observeWeeklySummary(): Flow<ReadingSummary> {
        val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return database.readingSessionDao().observeSummary(since).map { ReadingSummary(it.totalMillis, it.sessions, it.charactersRead) }
    }

    suspend fun getBook(id: String): Book? = books.getById(id)?.asExternalModel()
    suspend fun getChapters(bookId: String): List<Chapter> = chapters.getForBook(bookId).map { it.asExternalModel() }

    suspend fun readChapter(bookId: String, chapterId: Long): String = withContext(Dispatchers.IO) {
        cache[chapterId]?.let { return@withContext it }
        val book = books.getById(bookId) ?: error("书籍不存在")
        val chapter = chapters.getById(chapterId) ?: error("章节不存在")
        val parser = parsers.parserFor(BookFormat.valueOf(book.format))
        val text = parser.readChapter(File(book.privateFilePath), chapter.asParsed(), book.charset)
        cache.put(chapterId, text)
        text
    }

    suspend fun saveAnchor(anchor: ReaderAnchor, chapterIndex: Int) {
        books.updateProgress(
            bookId = anchor.bookId,
            chapter = chapterIndex,
            position = anchor.characterOffset,
            progress = anchor.percentage.coerceIn(0f, 1f),
            now = System.currentTimeMillis(),
        )
    }

    suspend fun toggleFavorite(bookId: String) = books.toggleFavorite(bookId)

    suspend fun updateBookMetadata(bookId: String, title: String, author: String) {
        books.updateMetadata(bookId, title.trim().ifBlank { "未命名书籍" }, author.trim().ifBlank { "未知作者" })
    }

    suspend fun markBookFinished(bookId: String) {
        val chapterList = getChapters(bookId)
        val lastIndex = chapterList.lastIndex.coerceAtLeast(0)
        val lastChapter = chapterList.getOrNull(lastIndex)
        val length = lastChapter?.let { readChapter(bookId, it.id).length } ?: 0
        if (lastChapter != null) saveAnchor(ReaderAnchor(bookId, lastChapter.id, length, 1f), lastIndex)
    }

    suspend fun resetBookProgress(bookId: String) {
        val first = getChapters(bookId).firstOrNull() ?: return
        saveAnchor(ReaderAnchor(bookId, first.id, 0, 0f), 0)
    }

    suspend fun addBookmark(bookId: String, chapterId: Long, characterOffset: Int, excerpt: String) =
        database.bookmarkDao().insert(
            BookmarkEntity(
                bookId = bookId,
                chapterId = chapterId,
                characterOffset = characterOffset,
                excerpt = excerpt.take(240),
                createdAt = System.currentTimeMillis(),
            )
        )

    suspend fun deleteBookmark(id: Long) = database.bookmarkDao().delete(id)

    suspend fun recordReadingSession(bookId: String, startedAt: Long, charactersRead: Long) {
        val endedAt = System.currentTimeMillis()
        if (endedAt - startedAt >= 5_000) {
            database.readingSessionDao().insert(ReadingSessionEntity(bookId = bookId, startedAt = startedAt, endedAt = endedAt, charactersRead = charactersRead))
        }
    }

    suspend fun reparseBook(bookId: String, charsetOverride: String? = null) = withContext(Dispatchers.IO) {
        val book = books.getById(bookId) ?: error("书籍不存在")
        val file = File(book.privateFilePath)
        if (!file.isFile) error("本地小说文件不存在")
        val parser = parsers.parserFor(BookFormat.valueOf(book.format))
        val parsed = parser.parse(ParseRequest(file, book.originalFileName, charsetOverride))
        database.withTransaction {
            database.searchDao().deleteFts(bookId)
            database.searchDao().deleteChunks(bookId)
            database.bookmarkDao().deleteForBook(bookId)
            database.annotationDao().deleteForBook(bookId)
            chapters.deleteForBook(bookId)
            chapters.insertAll(parsed.chapters.map { chapter ->
                ChapterEntity(
                    bookId = bookId,
                    index = chapter.index,
                    title = chapter.title,
                    locator = chapter.locator,
                    byteOffset = chapter.byteOffset,
                    byteLength = chapter.byteLength,
                    characterCount = chapter.characterCount,
                    synthetic = chapter.synthetic,
                )
            })
            books.updateAfterReparse(bookId, parsed.metadata.title, parsed.metadata.author, parsed.metadata.charset, parsed.metadata.totalCharacters, parsed.chapters.size)
        }
        cache.clear()
        indexBook(bookId)
    }

    suspend fun delete(ids: Set<String>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            books.getById(id)?.privateFilePath?.let { File(it).parentFile?.deleteRecursively() }
        }
        database.searchDao().let { dao -> ids.forEach { dao.deleteFts(it); dao.deleteChunks(it) } }
        books.deleteByIds(ids)
    }

    suspend fun indexBook(bookId: String) = withContext(Dispatchers.IO) {
        val book = books.getById(bookId) ?: return@withContext
        val chapterList = chapters.getForBook(bookId)
        val searchDao = database.searchDao()
        searchDao.deleteFts(bookId)
        searchDao.deleteChunks(bookId)
        val parser = parsers.parserFor(BookFormat.valueOf(book.format))
        chapterList.forEach { chapter ->
            val text = parser.readChapter(File(book.privateFilePath), chapter.asParsed(), book.charset)
            text.chunkedWithOffsets(SEARCH_CHUNK, SEARCH_OVERLAP).forEach { (offset, content) ->
                searchDao.insert(
                    SearchChunkEntity(
                        bookId = bookId,
                        chapterId = chapter.id,
                        chapterTitle = chapter.title,
                        startOffset = offset,
                        content = content,
                    ),
                    SearchTokenizer.tokenize(content),
                )
            }
        }
        books.setSearchIndexed(bookId, true)
    }

    suspend fun searchBook(bookId: String, rawQuery: String): List<SearchHit> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isBlank()) return@withContext emptyList()
        val match = SearchTokenizer.matchQuery(query)
        database.searchDao().search(bookId, match).mapNotNull { row ->
            val local = row.content.indexOf(query, ignoreCase = true)
            if (local < 0) null else {
                val start = (local - 32).coerceAtLeast(0)
                val end = (local + query.length + 48).coerceAtMost(row.content.length)
                SearchHit(
                    chapterId = row.chapterId,
                    chapterTitle = row.chapterTitle,
                    characterOffset = row.startOffset + local,
                    excerpt = row.content.substring(start, end).replace('\n', ' '),
                )
            }
        }
    }

    private fun ChapterEntity.asParsed() = ParsedChapter(index, title, locator, byteOffset, byteLength, characterCount, synthetic)

    companion object {
        private const val SEARCH_CHUNK = 800
        private const val SEARCH_OVERLAP = 80
    }
}

private object SearchTokenizer {
    fun tokenize(value: String): String {
        val output = StringBuilder(value.length * 2)
        val latin = StringBuilder()
        fun flushLatin() {
            if (latin.isNotEmpty()) {
                output.append(latin.toString().lowercase()).append(' ')
                latin.clear()
            }
        }
        value.forEach { char ->
            when {
                char.code in 0x3400..0x9FFF -> {
                    flushLatin()
                    output.append(char).append(' ')
                }
                char.isLetterOrDigit() -> latin.append(char)
                else -> flushLatin()
            }
        }
        flushLatin()
        return output.toString().trim()
    }

    fun matchQuery(query: String): String {
        val tokens = tokenize(query).replace("\"", "")
        return if (' ' in tokens) "\"$tokens\"" else "$tokens*"
    }
}

private fun String.chunkedWithOffsets(size: Int, overlap: Int): Sequence<Pair<Int, String>> = sequence {
    var offset = 0
    while (offset < length) {
        val end = (offset + size).coerceAtMost(length)
        yield(offset to substring(offset, end))
        if (end == length) break
        offset = (end - overlap).coerceAtLeast(offset + 1)
    }
}

private class ChapterCache(private val maxCharacters: Int) {
    private val entries = object : LinkedHashMap<Long, String>(16, .75f, true) {}
    private var characters = 0

    @Synchronized operator fun get(id: Long): String? = entries[id]

    @Synchronized fun put(id: Long, text: String) {
        entries.put(id, text)?.let { characters -= it.length }
        characters += text.length
        while (characters > maxCharacters && entries.isNotEmpty()) {
            val first = entries.entries.iterator().next()
            characters -= first.value.length
            entries.remove(first.key)
        }
    }

    @Synchronized fun clear() { entries.clear(); characters = 0 }
}
