package com.moyu.reader.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterDetectorTest {
    private val detector = ChapterDetector()

    @Test fun recognizesChineseArabicEnglishAndSpecialHeadings() {
        listOf("第一章 雨夜", "第 128 章：归途", "卷三　北境", "CHAPTER 12 - Arrival", "楔子", "番外二").forEachIndexed { index, title ->
            assertNotNull(title, detector.candidate(title, index * 1000L, previousBlank = true))
        }
    }

    @Test fun rejectsSentenceLikeFalsePositive() {
        assertNull(detector.candidate("第一章的故事到这里才刚刚开始。", 0, previousBlank = false))
        assertNull(detector.candidate("第一个章节，我们讨论很多事情，也有很多细节。", 0, previousBlank = true))
    }

    @Test fun requiresSeveralWellSpacedCandidates() {
        val candidates = listOf(
            ChapterCandidate("第一章", 100, .9f),
            ChapterCandidate("第二章", 2_000, .9f),
            ChapterCandidate("第三章", 4_500, .9f),
        )
        assertEquals(3, detector.select(candidates, 8_000).size)
        assertEquals(0, detector.select(candidates.take(1), 8_000).size)
    }
}

