package com.moyu.reader.parser

import com.moyu.reader.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import com.moyu.reader.util.readUpTo

class TxtBookParser(
    private val charsetDetector: CharsetDetector,
    private val chapterDetector: ChapterDetector,
) : BookParser {
    override val format = BookFormat.TXT

    override suspend fun canParse(file: File, originalFileName: String): Boolean =
        originalFileName.substringAfterLast('.', "").equals("txt", true)

    override suspend fun parse(request: ParseRequest): ParsedBook = withContext(Dispatchers.IO) {
        val detected = charsetDetector.detect(request.file, request.charsetOverride)
        val charset = Charset.forName(detected.name)
        val candidates = ArrayList<ChapterCandidate>()
        var previousBlank = true
        var characterCount = 0L
        ByteLineScanner(request.file, charset, detected.bomLength).use { scanner ->
            while (true) {
                coroutineContext.ensureActive()
                val line = scanner.nextLine() ?: break
                characterCount += line.estimatedCharacters
                if (!line.truncated) {
                    chapterDetector.candidate(line.text, line.offset, previousBlank)?.let(candidates::add)
                }
                previousBlank = line.text.isBlank()
            }
        }
        val selected = chapterDetector.select(candidates, request.file.length())
        val chapters = if (selected.isNotEmpty()) {
            buildDetectedChapters(selected, request.file.length(), charset)
        } else {
            buildSyntheticChapters(request.file.length(), detected.bomLength, charset)
        }
        val title = request.originalFileName.substringBeforeLast('.').trim().ifBlank { "未命名书籍" }
        ParsedBook(
            metadata = ParsedMetadata(
                title = title,
                author = "未知作者",
                format = format,
                charset = charset.name(),
                totalCharacters = characterCount,
            ),
            chapters = chapters,
        )
    }

    override suspend fun readChapter(file: File, chapter: ParsedChapter, charset: String?): String = withContext(Dispatchers.IO) {
        val cs = Charset.forName(charset ?: "UTF-8")
        FileInputStream(file).use { input ->
            input.channel.position(chapter.byteOffset)
            val max = chapter.byteLength.coerceAtMost(MAX_READ_BYTES.toLong()).toInt()
            val bytes = input.readUpTo(max)
            String(bytes, cs).trimStart('\uFEFF', '\r', '\n').replace("\r\n", "\n").withoutRepeatedHeading(chapter)
        }
    }

    private fun buildDetectedChapters(
        selected: List<ChapterCandidate>,
        fileSize: Long,
        charset: Charset,
    ): List<ParsedChapter> {
        val starts = buildList {
            if (selected.first().byteOffset > 256) add(ChapterCandidate("卷首", 0, 1f))
            addAll(selected)
        }
        val result = ArrayList<ParsedChapter>()
        starts.forEachIndexed { logicalIndex, candidate ->
            val end = starts.getOrNull(logicalIndex + 1)?.byteOffset ?: fileSize
            splitSpan(candidate.byteOffset, end, charset).forEachIndexed { partIndex, span ->
                result += ParsedChapter(
                    index = result.size,
                    title = if (partIndex == 0) candidate.title else "${candidate.title}（续 ${partIndex + 1}）",
                    locator = "bytes:${span.first}:${span.second}",
                    byteOffset = span.first,
                    byteLength = span.second,
                    characterCount = estimateCharacters(span.second, charset),
                    synthetic = partIndex > 0,
                )
            }
        }
        return result
    }

    private fun buildSyntheticChapters(fileSize: Long, bomLength: Int, charset: Charset): List<ParsedChapter> {
        val start = bomLength.toLong()
        val count = ceil((fileSize - start).coerceAtLeast(1).toDouble() / MAX_CHAPTER_BYTES).toInt()
        return (0 until count).map { index ->
            val offset = start + index * MAX_CHAPTER_BYTES
            val length = (fileSize - offset).coerceAtMost(MAX_CHAPTER_BYTES).coerceAtLeast(0)
            ParsedChapter(
                index = index,
                title = if (count == 1) "正文" else "正文 · ${index + 1}",
                locator = "bytes:$offset:$length",
                byteOffset = offset,
                byteLength = length,
                characterCount = estimateCharacters(length, charset),
                synthetic = true,
            )
        }
    }

    private fun splitSpan(start: Long, end: Long, charset: Charset): List<Pair<Long, Long>> {
        val result = ArrayList<Pair<Long, Long>>()
        var cursor = start
        while (cursor < end) {
            var length = (end - cursor).coerceAtMost(MAX_CHAPTER_BYTES)
            if (charset.name().startsWith("UTF-16") && length % 2L != 0L) length--
            if (length <= 0) break
            result += cursor to length
            cursor += length
        }
        return result
    }

    private fun estimateCharacters(bytes: Long, charset: Charset): Long = when {
        charset.name().startsWith("UTF-16") -> bytes / 2
        charset.name().equals("UTF-8", true) -> bytes / 2
        else -> bytes / 2
    }.coerceAtLeast(1)

    companion object {
        private const val MAX_CHAPTER_BYTES = 1024L * 1024L
        private const val MAX_READ_BYTES = 2 * 1024 * 1024
    }
}

private fun String.withoutRepeatedHeading(chapter: ParsedChapter): String {
    if (chapter.synthetic) return this
    val firstBreak = indexOf('\n')
    val firstLine = if (firstBreak >= 0) substring(0, firstBreak) else this
    fun String.normalized() = trim().replace(Regex("[\\t　 ]+"), " ").trimEnd('：', ':')
    return if (firstLine.normalized() == chapter.title.normalized()) {
        if (firstBreak >= 0) substring(firstBreak + 1).trimStart('\r', '\n') else ""
    } else this
}

private data class RawLine(
    val offset: Long,
    val text: String,
    val estimatedCharacters: Long,
    val truncated: Boolean,
)

private class ByteLineScanner(
    file: File,
    private val charset: Charset,
    bomLength: Int,
) : AutoCloseable {
    private val input = file.inputStream().buffered(64 * 1024)
    private var position = bomLength.toLong()
    private val utf16 = charset.name().startsWith("UTF-16")
    private val littleEndian = charset.name().equals("UTF-16LE", true)

    init {
        repeat(bomLength) { input.read() }
    }

    fun nextLine(): RawLine? = if (utf16) readUtf16Line() else readSingleByteNewline()

    private fun readSingleByteNewline(): RawLine? {
        val start = position
        val kept = ByteArrayOutputStream()
        var total = 0L
        var truncated = false
        while (true) {
            val value = input.read()
            if (value < 0) {
                if (total == 0L) return null
                break
            }
            position++
            if (value == 0x0A) break
            total++
            if (kept.size() < MAX_LINE_BYTES) kept.write(value) else truncated = true
        }
        val text = String(kept.toByteArray(), charset).trimEnd('\r')
        return RawLine(start, text, estimate(total), truncated)
    }

    private fun readUtf16Line(): RawLine? {
        val start = position
        val kept = ByteArrayOutputStream()
        var total = 0L
        var truncated = false
        while (true) {
            val a = input.read()
            val b = input.read()
            if (a < 0 || b < 0) {
                if (total == 0L) return null
                break
            }
            position += 2
            val codeUnit = if (littleEndian) a or (b shl 8) else (a shl 8) or b
            if (codeUnit == 0x000A) break
            total += 2
            if (kept.size() < MAX_LINE_BYTES) {
                kept.write(a)
                kept.write(b)
            } else truncated = true
        }
        val text = String(kept.toByteArray(), charset).trimEnd('\r')
        return RawLine(start, text, total / 2, truncated)
    }

    private fun estimate(bytes: Long): Long = when {
        charset.name().equals("UTF-8", true) -> (bytes / 2).coerceAtLeast(1)
        else -> (bytes / 2).coerceAtLeast(1)
    }

    override fun close() = input.close()

    companion object { private const val MAX_LINE_BYTES = 8192 }
}
