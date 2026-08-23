package com.moyu.reader.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.moyu.reader.data.db.AnnotationEntity
import com.moyu.reader.data.db.BookEntity
import com.moyu.reader.data.db.BookmarkEntity
import com.moyu.reader.data.db.ChapterEntity
import com.moyu.reader.data.db.MoyuDatabase
import com.moyu.reader.data.db.ReadingSessionEntity
import com.moyu.reader.data.preferences.SettingsRepository
import com.moyu.reader.model.PageAnimation
import com.moyu.reader.model.ReaderMode
import com.moyu.reader.model.ReaderTheme
import com.moyu.reader.model.ReaderOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupOptions(val includeSources: Boolean, val includeFonts: Boolean)
data class BackupResult(val books: Int, val bookmarks: Int, val bytes: Long)
data class RestoreResult(val restoredBooks: Int, val matchedBooks: Int, val skippedBooks: Int)

class BackupRepository(
    private val context: Context,
    private val database: MoyuDatabase,
    private val settings: SettingsRepository,
    private val library: LibraryRepository,
) {
    suspend fun create(destination: Uri, options: BackupOptions): BackupResult = withContext(Dispatchers.IO) {
        val snapshot = database.withTransaction {
            Snapshot(
                books = database.bookDao().getAllOnce(),
                chapters = database.chapterDao().getAllOnce(),
                bookmarks = database.bookmarkDao().getAllOnce(),
                annotations = database.annotationDao().getAllOnce(),
                sessions = database.readingSessionDao().getAllOnce(),
            )
        }
        val preferences = settings.preferences.first()
        var written = 0L
        context.contentResolver.openOutputStream(destination, "w")?.buffered(128 * 1024)?.use { output ->
            ZipOutputStream(output).use { zip ->
                val manifest = JSONObject()
                    .put("format", "moyu-backup")
                    .put("version", BACKUP_VERSION)
                    .put("createdAt", System.currentTimeMillis())
                    .put("includeSources", options.includeSources)
                    .put("includeFonts", options.includeFonts)
                written += zip.putBytes("manifest.json", manifest.toString(2).toByteArray())
                written += zip.putBytes("data.json", snapshot.toJson(preferences).toString().toByteArray())
                if (options.includeSources) {
                    snapshot.books.forEach { book ->
                        val source = File(book.privateFilePath)
                        if (source.isFile) written += zip.putFile("books/${book.id}/${source.name}", source)
                        book.coverPath?.let(::File)?.takeIf(File::isFile)?.let { cover -> written += zip.putFile("books/${book.id}/${cover.name}", cover) }
                    }
                }
                if (options.includeFonts) {
                    File(context.filesDir, "fonts").listFiles()?.filter(File::isFile)?.forEach { font ->
                        written += zip.putFile("fonts/${font.name}", font)
                    }
                }
            }
        } ?: error("系统没有授予写入权限")
        BackupResult(snapshot.books.size, snapshot.bookmarks.size, written)
    }

    suspend fun restore(source: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            extract(source, staging)
            val manifest = JSONObject(File(staging, "manifest.json").readText())
            require(manifest.getString("format") == "moyu-backup") { "这不是墨屿备份文件" }
            require(manifest.getInt("version") <= BACKUP_VERSION) { "备份来自更高版本的墨屿阅读" }
            val data = JSONObject(File(staging, "data.json").readText())
            val booksJson = data.getJSONArray("books")
            val chaptersJson = data.getJSONArray("chapters")
            val idMap = LinkedHashMap<String, String>()
            val chapterMap = LinkedHashMap<Long, Long>()
            var restored = 0
            var matched = 0
            var skipped = 0
            database.withTransaction {
                for (index in 0 until booksJson.length()) {
                    val json = booksJson.getJSONObject(index)
                    val oldId = json.getString("id")
                    val existing = database.bookDao().findByHash(json.getString("fileHash"))
                    if (existing != null) {
                        idMap[oldId] = existing.id
                        database.bookDao().restoreProgress(
                            existing.id,
                            json.optInt("currentChapter"),
                            json.optInt("currentPosition"),
                            json.optDouble("readingProgress").toFloat(),
                            json.optNullableLong("lastReadAt"),
                        )
                        matched++
                        val existingChapters = database.chapterDao().getForBook(existing.id).associateBy { it.index }
                        for (chapterIndex in 0 until chaptersJson.length()) {
                            val chapter = chaptersJson.getJSONObject(chapterIndex)
                            if (chapter.getString("bookId") == oldId) existingChapters[chapter.getInt("index")]?.let { chapterMap[chapter.getLong("id")] = it.id }
                        }
                        continue
                    }
                    val sourceDir = File(staging, "books/$oldId")
                    val sourceFile = sourceDir.listFiles()?.firstOrNull { it.name.startsWith("source.") }
                    if (sourceFile == null) { skipped++; continue }
                    val newId = UUID.randomUUID().toString()
                    val targetDir = File(context.filesDir, "books/$newId").apply { mkdirs() }
                    val targetSource = File(targetDir, sourceFile.name)
                    sourceFile.copyTo(targetSource, overwrite = true)
                    val coverSource = sourceDir.listFiles()?.firstOrNull { it.name.startsWith("cover.") }
                    val coverTarget = coverSource?.let { File(targetDir, it.name).also { target -> it.copyTo(target, overwrite = true) } }
                    database.bookDao().insert(json.toBookEntity(newId, targetSource.absolutePath, coverTarget?.absolutePath))
                    val restoredChapters = buildList {
                        for (chapterIndex in 0 until chaptersJson.length()) {
                            val chapter = chaptersJson.getJSONObject(chapterIndex)
                            if (chapter.getString("bookId") == oldId) add(chapter.toChapterEntity(newId))
                        }
                    }
                    val newChapterIds = database.chapterDao().insertAll(restoredChapters)
                    var restoredChapterCursor = 0
                    for (chapterIndex in 0 until chaptersJson.length()) {
                        val chapter = chaptersJson.getJSONObject(chapterIndex)
                        if (chapter.getString("bookId") == oldId) chapterMap[chapter.getLong("id")] = newChapterIds[restoredChapterCursor++]
                    }
                    idMap[oldId] = newId
                    restored++
                }
                restoreBookmarks(data.optJSONArray("bookmarks"), idMap, chapterMap)
                restoreAnnotations(data.optJSONArray("annotations"), idMap, chapterMap)
                restoreSessions(data.optJSONArray("sessions"), idMap)
            }
            File(staging, "fonts").takeIf(File::isDirectory)?.copyRecursively(File(context.filesDir, "fonts"), overwrite = true)
            restorePreferences(data.optJSONObject("settings"))
            idMap.values.forEach { runCatching { library.indexBook(it) } }
            RestoreResult(restored, matched, skipped)
        } finally {
            staging.deleteRecursively()
        }
    }

    private suspend fun restorePreferences(json: JSONObject?) {
        if (json == null) return
        json.optString("theme").takeIf(String::isNotBlank)?.let { runCatching { settings.setTheme(ReaderTheme.valueOf(it)) } }
        json.optString("mode").takeIf(String::isNotBlank)?.let { runCatching { settings.setReaderMode(ReaderMode.valueOf(it)) } }
        json.optString("pageAnimation").takeIf(String::isNotBlank)?.let { runCatching { settings.setPageAnimation(PageAnimation.valueOf(it)) } }
        settings.setPageTurnDuration(json.optInt("pageTurnDurationMs", 300))
        json.optString("orientation").takeIf(String::isNotBlank)?.let { runCatching { settings.setOrientation(ReaderOrientation.valueOf(it)) } }
        settings.setFontSize(json.optDouble("fontSizeSp", 19.0).toFloat())
        settings.setFontWeight(json.optInt("fontWeight", 400))
        settings.setLineHeight(json.optDouble("lineHeightMultiplier", 1.68).toFloat())
        settings.setHorizontalMargin(json.optDouble("horizontalMarginDp", 24.0).toFloat())
        settings.setParagraphSpacing(json.optDouble("paragraphSpacingDp", 2.0).toFloat())
        settings.setFirstLineIndent(json.optDouble("firstLineIndentEm", 0.0).toFloat())
        settings.setJustified(json.optBoolean("justified", false))
        settings.setKeepScreenOn(json.optBoolean("keepScreenOn", false))
        settings.setBrightness(json.optDouble("brightness", -1.0).toFloat())
        settings.setReducedMotion(json.optBoolean("reducedMotion", false))
        settings.setTtsRate(json.optDouble("ttsRate", 1.0).toFloat())
        settings.setShowReaderClock(json.optBoolean("showReaderClock", true))
        settings.setVolumeKeyPageTurn(json.optBoolean("volumeKeyPageTurn", true))
        json.optString("customFontFile").takeIf(String::isNotBlank)?.let { name ->
            File(context.filesDir, "fonts/$name").takeIf(File::isFile)?.let { settings.setCustomFont(it.absolutePath) }
        }
        settings.completeOnboarding()
    }

    private suspend fun restoreBookmarks(array: JSONArray?, idMap: Map<String, String>, chapterMap: Map<Long, Long>) {
        if (array == null) return
        val entities = buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                val mapped = idMap[json.getString("bookId")] ?: continue
                val chapterId = chapterMap[json.getLong("chapterId")] ?: continue
                add(BookmarkEntity(bookId = mapped, chapterId = chapterId, characterOffset = json.getInt("characterOffset"), excerpt = json.getString("excerpt"), createdAt = json.getLong("createdAt")))
            }
        }
        database.bookmarkDao().insertAll(entities)
    }

    private suspend fun restoreAnnotations(array: JSONArray?, idMap: Map<String, String>, chapterMap: Map<Long, Long>) {
        if (array == null) return
        val entities = buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                val mapped = idMap[json.getString("bookId")] ?: continue
                val chapterId = chapterMap[json.getLong("chapterId")] ?: continue
                add(AnnotationEntity(bookId = mapped, chapterId = chapterId, startOffset = json.getInt("startOffset"), endOffset = json.getInt("endOffset"), selectedText = json.getString("selectedText"), note = json.optString("note"), color = json.optInt("color"), createdAt = json.getLong("createdAt"), updatedAt = json.getLong("updatedAt")))
            }
        }
        database.annotationDao().insertAll(entities)
    }

    private suspend fun restoreSessions(array: JSONArray?, idMap: Map<String, String>) {
        if (array == null) return
        val entities = buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                val mapped = idMap[json.getString("bookId")] ?: continue
                add(ReadingSessionEntity(bookId = mapped, startedAt = json.getLong("startedAt"), endedAt = json.getLong("endedAt"), charactersRead = json.getLong("charactersRead")))
            }
        }
        database.readingSessionDao().insertAll(entities)
    }

    private fun extract(uri: Uri, staging: File) {
        var total = 0L
        context.contentResolver.openInputStream(uri)?.buffered(128 * 1024)?.use { source ->
            ZipInputStream(source).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val output = File(staging, entry.name)
                    val canonical = output.canonicalFile
                    require(canonical.path.startsWith(staging.canonicalPath + File.separator)) { "备份包含非法路径" }
                    canonical.parentFile?.mkdirs()
                    FileOutputStream(canonical).use { target ->
                        val buffer = ByteArray(128 * 1024)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_RESTORE_BYTES) { "备份展开后超过 8 GB" }
                            target.write(buffer, 0, count)
                        }
                    }
                }
            }
        } ?: error("系统没有授予读取权限")
    }

    private data class Snapshot(
        val books: List<BookEntity>,
        val chapters: List<ChapterEntity>,
        val bookmarks: List<BookmarkEntity>,
        val annotations: List<AnnotationEntity>,
        val sessions: List<ReadingSessionEntity>,
    )

    private fun Snapshot.toJson(preferences: com.moyu.reader.data.preferences.AppPreferences) = JSONObject().apply {
        put("books", JSONArray().apply { books.forEach { put(it.toJson()) } })
        put("chapters", JSONArray().apply { chapters.forEach { put(it.toJson()) } })
        put("bookmarks", JSONArray().apply { bookmarks.forEach { put(it.toJson()) } })
        put("annotations", JSONArray().apply { annotations.forEach { put(it.toJson()) } })
        put("sessions", JSONArray().apply { sessions.forEach { put(it.toJson()) } })
        put("settings", JSONObject().apply {
            put("theme", preferences.reader.theme.name); put("mode", preferences.reader.mode.name); put("pageAnimation", preferences.reader.pageAnimation.name); put("pageTurnDurationMs", preferences.reader.pageTurnDurationMs); put("orientation", preferences.reader.orientation.name)
            put("fontSizeSp", preferences.reader.fontSizeSp); put("fontWeight", preferences.reader.fontWeight); put("lineHeightMultiplier", preferences.reader.lineHeightMultiplier); put("horizontalMarginDp", preferences.reader.horizontalMarginDp); put("paragraphSpacingDp", preferences.reader.paragraphSpacingDp); put("firstLineIndentEm", preferences.reader.firstLineIndentEm); put("justified", preferences.reader.justified)
            put("keepScreenOn", preferences.reader.keepScreenOn); put("brightness", preferences.reader.brightness); put("reducedMotion", preferences.reader.reducedMotion); put("customFontFile", preferences.reader.customFontPath?.let(::File)?.name ?: "")
            put("ttsRate", preferences.reader.ttsRate); put("showReaderClock", preferences.reader.showReaderClock); put("volumeKeyPageTurn", preferences.reader.volumeKeyPageTurn)
        })
    }

    private fun BookEntity.toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("author", author); put("format", format); put("originalFileName", originalFileName); put("fileSize", fileSize); put("fileHash", fileHash)
        put("charset", charset ?: JSONObject.NULL); put("addedAt", addedAt); put("lastReadAt", lastReadAt ?: JSONObject.NULL); put("totalCharacters", totalCharacters); put("chapterCount", chapterCount)
        put("readingProgress", readingProgress); put("currentChapter", currentChapter); put("currentPosition", currentPosition); put("favorite", favorite)
    }
    private fun ChapterEntity.toJson() = JSONObject().apply { put("id", id); put("bookId", bookId); put("index", index); put("title", title); put("locator", locator); put("byteOffset", byteOffset); put("byteLength", byteLength); put("characterCount", characterCount); put("synthetic", synthetic) }
    private fun BookmarkEntity.toJson() = JSONObject().apply { put("bookId", bookId); put("chapterId", chapterId); put("characterOffset", characterOffset); put("excerpt", excerpt); put("createdAt", createdAt) }
    private fun AnnotationEntity.toJson() = JSONObject().apply { put("bookId", bookId); put("chapterId", chapterId); put("startOffset", startOffset); put("endOffset", endOffset); put("selectedText", selectedText); put("note", note); put("color", color); put("createdAt", createdAt); put("updatedAt", updatedAt) }
    private fun ReadingSessionEntity.toJson() = JSONObject().apply { put("bookId", bookId); put("startedAt", startedAt); put("endedAt", endedAt); put("charactersRead", charactersRead) }

    private fun JSONObject.toBookEntity(id: String, path: String, cover: String?) = BookEntity(
        id, getString("title"), getString("author"), getString("format"), getString("originalFileName"), path, getLong("fileSize"), getString("fileHash"), optNullableString("charset"),
        getLong("addedAt"), optNullableLong("lastReadAt"), getLong("totalCharacters"), getInt("chapterCount"), optDouble("readingProgress").toFloat(), optInt("currentChapter"), optInt("currentPosition"), cover, optBoolean("favorite"), false,
    )
    private fun JSONObject.toChapterEntity(bookId: String) = ChapterEntity(bookId = bookId, index = getInt("index"), title = getString("title"), locator = getString("locator"), byteOffset = getLong("byteOffset"), byteLength = getLong("byteLength"), characterCount = getLong("characterCount"), synthetic = optBoolean("synthetic"))
    private fun JSONObject.optNullableString(name: String): String? = if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)
    private fun JSONObject.optNullableLong(name: String): Long? = if (isNull(name) || !has(name)) null else getLong(name)

    private fun ZipOutputStream.putBytes(name: String, bytes: ByteArray): Long { putNextEntry(ZipEntry(name)); write(bytes); closeEntry(); return bytes.size.toLong() }
    private fun ZipOutputStream.putFile(name: String, file: File): Long { putNextEntry(ZipEntry(name)); file.inputStream().buffered(128 * 1024).use { it.copyTo(this, 128 * 1024) }; closeEntry(); return file.length() }

    companion object {
        private const val BACKUP_VERSION = 1
        private const val MAX_RESTORE_BYTES = 8L * 1024 * 1024 * 1024
    }
}
