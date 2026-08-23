package com.moyu.reader

import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moyu.reader.data.ImportResult
import com.moyu.reader.model.ReaderAnchor
import com.moyu.reader.reader.AndroidPaginator
import com.moyu.reader.reader.PaginationSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImportReadPersistenceTest {
    private val app = ApplicationProvider.getApplicationContext<MoyuApplication>()
    private val container get() = app.container

    @Test fun importTxtReadAndRestoreSemanticPosition() = runBlocking {
        val uri = assetUri("small_utf8.txt")
        val result = container.importer.import(uri)
        val id = when (result) {
            is ImportResult.Success -> result.bookId
            is ImportResult.Duplicate -> result.existingBookId
            is ImportResult.Failure -> error(result.message)
        }
        val chapters = container.library.getChapters(id)
        assertTrue(chapters.isNotEmpty())
        val text = container.library.readChapter(id, chapters.first().id)
        assertTrue(text.contains("雨落在旧城"))
        container.library.saveAnchor(ReaderAnchor(id, chapters.first().id, 42, .25f), 0)
        val persisted = container.library.getBook(id)!!
        assertEquals(42, persisted.currentPosition)
        assertEquals(.25f, persisted.readingProgress, .001f)
    }

    @Test fun importEpubParsesSpineAndSearchIndex() = runBlocking {
        val result = container.importer.import(assetUri("test_epub3.epub"))
        val id = when (result) {
            is ImportResult.Success -> result.bookId
            is ImportResult.Duplicate -> result.existingBookId
            is ImportResult.Failure -> error(result.message)
        }
        val chapters = container.library.getChapters(id)
        assertEquals(2, chapters.size)
        val hits = container.library.searchBook(id, "旧城")
        assertTrue(hits.isNotEmpty())
    }

    @Test fun androidPaginatorCreatesMultiplePagesForLongChineseChapter() {
        val text = List(80) { "雨落在旧城的屋檐上，像有人翻动一本很厚的书。" }.joinToString("\n")
        val pages = AndroidPaginator().paginate(
            text,
            PaginationSpec(
                viewportWidthPx = 900,
                viewportHeightPx = 1_700,
                fontSizePx = 52f,
                lineHeightMultiplier = 1.8f,
                paragraphSpacingPx = 0f,
                pageReservedPx = 100,
                firstPageReservedPx = 180,
            ),
        )
        assertTrue("Long chapter should be split into real pages", pages.size > 2)
        assertEquals(0, pages.first().start)
        assertEquals(text.length, pages.last().endExclusive)
        pages.zipWithNext().forEach { (first, second) -> assertEquals(first.endExclusive, second.start) }
        pages.dropLast(1).forEach { page ->
            val finalCharacter = text.substring(0, page.endExclusive).trimEnd().last()
            assertTrue("A natural page break should finish on sentence punctuation", finalCharacter in "。！？；…!?;")
        }

        val roomyPages = AndroidPaginator().paginate(
            text,
            PaginationSpec(
                viewportWidthPx = 900,
                viewportHeightPx = 1_700,
                fontSizePx = 52f,
                lineHeightMultiplier = 1.8f,
                paragraphSpacingPx = 36f,
                pageReservedPx = 100,
                firstPageReservedPx = 180,
            ),
        )
        assertTrue("Paragraph spacing must visibly affect pagination", roomyPages.size > pages.size)
    }

    private fun assetUri(name: String): android.net.Uri {
        val directory = File(app.cacheDir, "import-test").apply { mkdirs() }
        val file = File(directory, name)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open(name).use { input ->
            file.outputStream().use(input::copyTo)
        }
        return FileProvider.getUriForFile(app, "${app.packageName}.files", file)
    }
}
