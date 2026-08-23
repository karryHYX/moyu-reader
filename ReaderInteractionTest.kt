package com.moyu.reader.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderInteractionTest {
    @Test
    fun `left center and right taps map to stable zones`() {
        assertEquals(ReaderGestureAction.PREVIOUS_PAGE, ReaderInteraction.tapAction(20f, 400f))
        assertEquals(ReaderGestureAction.TOGGLE_CONTROLS, ReaderInteraction.tapAction(200f, 400f))
        assertEquals(ReaderGestureAction.NEXT_PAGE, ReaderInteraction.tapAction(380f, 400f))
    }

    @Test
    fun `horizontal swipes turn pages and vertical gestures are ignored`() {
        assertEquals(ReaderGestureAction.NEXT_PAGE, ReaderInteraction.swipeAction(-120f, 18f, 400f))
        assertEquals(ReaderGestureAction.PREVIOUS_PAGE, ReaderInteraction.swipeAction(120f, 18f, 400f))
        assertEquals(ReaderGestureAction.NONE, ReaderInteraction.swipeAction(16f, 140f, 400f))
        assertEquals(ReaderGestureAction.NONE, ReaderInteraction.swipeAction(20f, 2f, 400f))
    }

    @Test
    fun `semantic offset restores first middle and final page`() {
        val pages = listOf(PageSlice(0, 100), PageSlice(100, 200), PageSlice(200, 280))
        assertEquals(0, ReaderInteraction.pageForOffset(pages, 0))
        assertEquals(1, ReaderInteraction.pageForOffset(pages, 145))
        assertEquals(2, ReaderInteraction.pageForOffset(pages, Int.MAX_VALUE))
    }
}
