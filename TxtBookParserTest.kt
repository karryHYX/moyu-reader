package com.moyu.reader.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class TxtBookParserTest {
    private val parser = TxtBookParser(CharsetDetector(), ChapterDetector())

    @Test fun parsesMultipleChapterRulesAndReadsBySpan() = runBlocking {
        val file = kotlin.io.path.createTempFile(suffix = ".txt").toFile()
        try {
            val body = buildString {
                appendLine("第一章 雨夜"); repeat(80) { appendLine("雨落在旧城的屋檐上，像有人翻动一本很厚的书。") }
                appendLine(); appendLine("第2章　清晨"); repeat(80) { appendLine("天色从河的另一边慢慢亮起来。") }
                appendLine(); appendLine("CHAPTER 3 - RETURN"); repeat(80) { appendLine("他沿着旧路回到城里。") }
            }
            file.writeText(body, Charset.forName("GB18030"))
            val parsed = parser.parse(ParseRequest(file, "山海之间.txt", "GB18030"))
            assertTrue(parsed.chapters.size >= 3)
            assertEquals("山海之间", parsed.metadata.title)
            val first = parser.readChapter(file, parsed.chapters.first(), parsed.metadata.charset)
            assertTrue(first.contains("雨落在旧城"))
            assertTrue(!first.trimStart().startsWith("第一章 雨夜"))
        } finally { file.delete() }
    }

    @Test fun noChapterFileStillBecomesReadableChunks() = runBlocking {
        val file = kotlin.io.path.createTempFile(suffix = ".txt").toFile()
        try {
            file.writeText("这是一段没有章节标题的正文。\n".repeat(100))
            val parsed = parser.parse(ParseRequest(file, "无章节.txt"))
            assertTrue(parsed.chapters.isNotEmpty())
            assertTrue(parsed.chapters.all { it.synthetic })
        } finally { file.delete() }
    }
}
