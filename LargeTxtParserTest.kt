package com.moyu.reader.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.BufferedOutputStream
import kotlin.system.measureTimeMillis

@RunWith(Parameterized::class)
class LargeTxtParserTest(private val sizeMiB: Int) {
    @Test fun streamParseAndRandomChapterReadDoNotDependOnWholeBookMemory() = runBlocking {
        val file = kotlin.io.path.createTempFile(suffix = "-${sizeMiB}mb.txt").toFile()
        try {
            val target = sizeMiB.toLong() * 1024 * 1024
            val paragraph = "远处的灯火隔着薄雾，一盏一盏亮起来。山风穿过松林，带来潮湿的气味。\n".toByteArray()
            BufferedOutputStream(file.outputStream(), 256 * 1024).use { output ->
                var written = 0L
                var chapter = 1
                while (written < target) {
                    val heading = "\n第${chapter++}章　压力测试\n".toByteArray()
                    output.write(heading); written += heading.size
                    repeat(1_300) {
                        if (written < target) { output.write(paragraph); written += paragraph.size }
                    }
                }
            }
            val parser = TxtBookParser(CharsetDetector(), ChapterDetector())
            lateinit var parsed: ParsedBook
            val elapsed = measureTimeMillis { parsed = parser.parse(ParseRequest(file, "long-$sizeMiB.txt", "UTF-8")) }
            assertTrue(parsed.chapters.size >= 2)
            assertTrue(parser.readChapter(file, parsed.chapters.first(), parsed.metadata.charset).isNotBlank())
            assertTrue(parser.readChapter(file, parsed.chapters.last(), parsed.metadata.charset).isNotBlank())
            println("LARGE_TXT sizeMiB=$sizeMiB elapsedMs=$elapsed chapters=${parsed.chapters.size}")
        } finally {
            file.delete()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} MiB")
        fun sizes() = listOf(arrayOf(1), arrayOf(10), arrayOf(50), arrayOf(100))
    }
}

