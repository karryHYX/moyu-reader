package com.moyu.reader.parser

import com.moyu.reader.model.BookFormat
import java.io.File

data class ParseRequest(
    val file: File,
    val originalFileName: String,
    val charsetOverride: String? = null,
)

data class ParsedMetadata(
    val title: String,
    val author: String,
    val format: BookFormat,
    val charset: String?,
    val totalCharacters: Long,
    val coverBytes: ByteArray? = null,
    val coverExtension: String? = null,
)

data class ParsedChapter(
    val index: Int,
    val title: String,
    val locator: String,
    val byteOffset: Long = 0,
    val byteLength: Long = 0,
    val characterCount: Long = 0,
    val synthetic: Boolean = false,
)

data class ParsedBook(
    val metadata: ParsedMetadata,
    val chapters: List<ParsedChapter>,
)

interface BookParser {
    val format: BookFormat
    suspend fun canParse(file: File, originalFileName: String): Boolean
    suspend fun parse(request: ParseRequest): ParsedBook
    suspend fun readChapter(file: File, chapter: ParsedChapter, charset: String?): String
}

class ParserException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ParserRegistry(private val parsers: Set<BookParser>) {
    suspend fun parserFor(file: File, originalFileName: String): BookParser =
        parsers.firstOrNull { it.canParse(file, originalFileName) }
            ?: throw ParserException("暂不支持这个文件格式")

    fun parserFor(format: BookFormat): BookParser =
        parsers.firstOrNull { it.format == format }
            ?: error("Parser missing for $format")
}

