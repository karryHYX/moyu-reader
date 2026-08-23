package com.moyu.reader.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubBookParserTest {
    private val parser = EpubBookParser()

    @Test fun parsesEpub3MetadataSpineNavAndText() = runBlocking {
        val file = createEpub()
        try {
            val parsed = parser.parse(ParseRequest(file, "test_epub3.epub"))
            assertEquals("山海之间", parsed.metadata.title)
            assertEquals("林舟", parsed.metadata.author)
            assertEquals(2, parsed.chapters.size)
            assertEquals("雨夜来信", parsed.chapters.first().title)
            assertTrue(parser.readChapter(file, parsed.chapters.first(), null).contains("旧城"))
        } finally { file.delete() }
    }

    private fun createEpub(): File {
        val file = kotlin.io.path.createTempFile(suffix = ".epub").toFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            fun put(name: String, content: String) { zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray()); zip.closeEntry() }
            put("mimetype", "application/epub+zip")
            put("META-INF/container.xml", """<?xml version="1.0"?><container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""")
            put("OEBPS/content.opf", """<?xml version="1.0"?><package><metadata><dc:title xmlns:dc="dc">山海之间</dc:title><dc:creator xmlns:dc="dc">林舟</dc:creator></metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="c1" href="c1.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="c2.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""")
            put("OEBPS/nav.xhtml", """<html><body><nav><ol><li><a href="c1.xhtml">雨夜来信</a></li><li><a href="c2.xhtml">清晨</a></li></ol></nav></body></html>""")
            put("OEBPS/c1.xhtml", """<html><body><h1>雨夜来信</h1><p>雨落在旧城的屋檐上。</p></body></html>""")
            put("OEBPS/c2.xhtml", """<html><body><h1>清晨</h1><p>灯火在薄雾中熄灭。</p></body></html>""")
        }
        return file
    }
}

