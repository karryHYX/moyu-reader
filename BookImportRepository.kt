package com.moyu.reader.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.moyu.reader.data.db.BookEntity
import com.moyu.reader.data.db.ChapterEntity
import com.moyu.reader.data.db.MoyuDatabase
import com.moyu.reader.parser.ParseRequest
import com.moyu.reader.parser.ParserException
import com.moyu.reader.parser.ParserRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext

data class ImportProgress(
    val fileName: String,
    val stage: Stage,
    val fraction: Float?,
) {
    enum class Stage { COPYING, HASHING, PARSING, SAVING, INDEXING }
}

sealed interface ImportResult {
    data class Success(val bookId: String, val title: String) : ImportResult
    data class Duplicate(val existingBookId: String, val title: String) : ImportResult
    data class Failure(val fileName: String, val message: String) : ImportResult
}

class BookImportRepository(
    private val context: Context,
    private val database: MoyuDatabase,
    private val parsers: ParserRegistry,
    private val library: LibraryRepository,
) {
    suspend fun import(
        uri: Uri,
        charsetOverride: String? = null,
        onProgress: (ImportProgress) -> Unit = {},
    ): ImportResult = withContext(Dispatchers.IO) {
        val info = queryInfo(context.contentResolver, uri)
        val stagingDir = File(context.cacheDir, "import-staging").apply { mkdirs() }
        val staging = File(stagingDir, "${UUID.randomUUID()}.part")
        try {
            ensureSpace(info.size)
            onProgress(ImportProgress(info.name, ImportProgress.Stage.COPYING, 0f))
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            context.contentResolver.openInputStream(uri)?.use { raw ->
                DigestInputStream(raw.buffered(128 * 1024), digest).use { input ->
                    staging.outputStream().buffered(128 * 1024).use { output ->
                        val buffer = ByteArray(128 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            copied += count
                            val fraction = info.size?.takeIf { it > 0 }?.let { copied.toFloat() / it }
                            onProgress(ImportProgress(info.name, ImportProgress.Stage.COPYING, fraction?.coerceIn(0f, 1f)))
                        }
                    }
                }
            } ?: return@withContext ImportResult.Failure(info.name, "系统没有授予读取权限")
            if (copied == 0L) return@withContext ImportResult.Failure(info.name, "文件是空的")
            onProgress(ImportProgress(info.name, ImportProgress.Stage.HASHING, 1f))
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            database.bookDao().findByHash(hash)?.let {
                return@withContext ImportResult.Duplicate(it.id, it.title)
            }

            val parser = parsers.parserFor(staging, info.name)
            onProgress(ImportProgress(info.name, ImportProgress.Stage.PARSING, null))
            val parsed = parser.parse(ParseRequest(staging, info.name, charsetOverride))
            val id = UUID.randomUUID().toString()
            val extension = when (parsed.metadata.format) {
                com.moyu.reader.model.BookFormat.TXT -> "txt"
                com.moyu.reader.model.BookFormat.EPUB -> "epub"
            }
            val bookDir = File(context.filesDir, "books/$id").apply { mkdirs() }
            val source = File(bookDir, "source.$extension")
            if (!staging.renameTo(source)) {
                staging.copyTo(source, overwrite = true)
                staging.delete()
            }
            val coverPath = parsed.metadata.coverBytes?.let { bytes ->
                val cover = File(bookDir, "cover.${parsed.metadata.coverExtension ?: "jpg"}")
                cover.writeBytes(bytes)
                cover.absolutePath
            }
            onProgress(ImportProgress(info.name, ImportProgress.Stage.SAVING, null))
            val now = System.currentTimeMillis()
            database.withTransaction {
                database.bookDao().insert(
                    BookEntity(
                        id = id,
                        title = parsed.metadata.title,
                        author = parsed.metadata.author,
                        format = parsed.metadata.format.name,
                        originalFileName = info.name,
                        privateFilePath = source.absolutePath,
                        fileSize = source.length(),
                        fileHash = hash,
                        charset = parsed.metadata.charset,
                        addedAt = now,
                        lastReadAt = null,
                        totalCharacters = parsed.metadata.totalCharacters,
                        chapterCount = parsed.chapters.size,
                        readingProgress = 0f,
                        currentChapter = 0,
                        currentPosition = 0,
                        coverPath = coverPath,
                        favorite = false,
                        searchIndexed = false,
                    )
                )
                database.chapterDao().insertAll(parsed.chapters.map { chapter ->
                    ChapterEntity(
                        bookId = id,
                        index = chapter.index,
                        title = chapter.title,
                        locator = chapter.locator,
                        byteOffset = chapter.byteOffset,
                        byteLength = chapter.byteLength,
                        characterCount = chapter.characterCount,
                        synthetic = chapter.synthetic,
                    )
                })
            }
            onProgress(ImportProgress(info.name, ImportProgress.Stage.INDEXING, null))
            runCatching { library.indexBook(id) }
            ImportResult.Success(id, parsed.metadata.title)
        } catch (error: ParserException) {
            ImportResult.Failure(info.name, error.message ?: "解析没有完成")
        } catch (error: SecurityException) {
            ImportResult.Failure(info.name, "文件权限已经失效，请重新选择")
        } catch (error: Exception) {
            ImportResult.Failure(info.name, error.message?.take(140) ?: "导入没有完成")
        } finally {
            staging.delete()
        }
    }

    suspend fun scanTree(treeUri: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        buildList { scan(root, this, depth = 0) }
    }

    private fun scan(node: DocumentFile, output: MutableList<Uri>, depth: Int) {
        if (depth > 12) return
        node.listFiles().forEach { child ->
            when {
                child.isDirectory -> scan(child, output, depth + 1)
                child.isFile && child.name?.substringAfterLast('.', "")?.lowercase() in setOf("txt", "epub") -> output += child.uri
            }
        }
    }

    private fun ensureSpace(sourceSize: Long?) {
        if (sourceSize == null || sourceSize <= 0) return
        val available = StatFs(context.filesDir.absolutePath).availableBytes
        if (available < sourceSize * 2 + 32L * 1024 * 1024) throw IllegalStateException("设备存储空间不足")
    }

    private data class SourceInfo(val name: String, val size: Long?)

    private fun queryInfo(resolver: ContentResolver, uri: Uri): SourceInfo {
        var name: String? = null
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
        return SourceInfo(name?.takeIf { it.isNotBlank() } ?: "未命名文件", size)
    }
}
