package com.moyu.reader.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderMathTest {
    @Test fun progressSurvivesRepaginationBecauseItUsesCharacterOffset() {
        assertEquals(.375f, ReadingProgress.calculate(1, 4, 500, 1000), .0001f)
        assertEquals(1f, ReadingProgress.calculate(3, 4, 1000, 1000), .0001f)
    }

    @Test fun pageBreaksAlwaysAdvanceAndCoverAllText() {
        val pages = PageBreakCalculator().calculate(1_001) { start -> start + 120 }
        assertEquals(0, pages.first().start)
        assertEquals(1_001, pages.last().endExclusive)
        pages.zipWithNext().forEach { (a, b) -> assertEquals(a.endExclusive, b.start) }
    }
}

