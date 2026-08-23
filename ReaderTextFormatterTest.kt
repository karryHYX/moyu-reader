package com.moyu.reader.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTextFormatterTest {
    @Test
    fun `leading source whitespace is hidden without shifting anchors`() {
        val source = "　　第一段\n  第二段\n\t第三段"
        val formatted = ReaderTextFormatter.forDisplay(source)

        assertEquals(source.length, formatted.length)
        assertFalse(formatted.startsWith("　"))
        assertEquals('第', formatted[2])
        assertEquals('第', formatted[source.indexOf("第二段")])
        assertEquals('第', formatted[source.indexOf("第三段")])
    }

    @Test
    fun `paragraph starts are identified at chapter and newline boundaries`() {
        val text = "甲\n乙"
        assertTrue(ReaderTextFormatter.isParagraphStart(text, 0))
        assertTrue(ReaderTextFormatter.isParagraphStart(text, 2))
        assertFalse(ReaderTextFormatter.isParagraphStart(text, 1))
    }
}
