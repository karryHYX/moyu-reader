package com.moyu.reader.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class CharsetDetectorTest {
    private val detector = CharsetDetector()
    private val text = "第一章 雨夜\n雨落在旧城的屋檐上，远处的灯火亮了起来。\n第二章 清晨"

    @Test fun detectsUtf8Bom() = withTemp(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray()) {
        val result = detector.detect(it)
        assertEquals("UTF-8", result.name)
        assertEquals(3, result.bomLength)
    }

    @Test fun respectsManualBig5Override() = withTemp(text.toByteArray(Charset.forName("Big5"))) {
        assertTrue(detector.detect(it, "Big5").name.contains("Big5", ignoreCase = true))
    }

    @Test fun detectsUtf16Bom() = withTemp(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)) {
        assertEquals("UTF-16LE", detector.detect(it).name)
    }

    private fun withTemp(bytes: ByteArray, block: (File) -> Unit) {
        val file = kotlin.io.path.createTempFile(suffix = ".txt").toFile()
        try { file.writeBytes(bytes); block(file) } finally { file.delete() }
    }
}

