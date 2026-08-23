package com.moyu.reader.reader

import kotlin.math.abs

/**
 * Pure decision layer for the reader's three-zone tap and horizontal swipe gestures.
 * Keeping this outside Compose makes boundary behaviour deterministic and testable.
 */
object ReaderInteraction {
    const val LEFT_ZONE_FRACTION = .30f
    const val RIGHT_ZONE_FRACTION = .70f

    fun tapAction(x: Float, width: Float): ReaderGestureAction = when {
        width <= 0f -> ReaderGestureAction.TOGGLE_CONTROLS
        x < width * LEFT_ZONE_FRACTION -> ReaderGestureAction.PREVIOUS_PAGE
        x > width * RIGHT_ZONE_FRACTION -> ReaderGestureAction.NEXT_PAGE
        else -> ReaderGestureAction.TOGGLE_CONTROLS
    }

    fun swipeAction(deltaX: Float, deltaY: Float, width: Float): ReaderGestureAction {
        val threshold = (width * .14f).coerceAtLeast(48f)
        if (abs(deltaX) < threshold || abs(deltaX) <= abs(deltaY) * 1.25f) {
            return ReaderGestureAction.NONE
        }
        return if (deltaX < 0f) ReaderGestureAction.NEXT_PAGE else ReaderGestureAction.PREVIOUS_PAGE
    }

    fun pageForOffset(pages: List<PageSlice>, characterOffset: Int): Int =
        pages.indexOfLast { it.start <= characterOffset }.coerceAtLeast(0)
}

enum class ReaderGestureAction { NONE, PREVIOUS_PAGE, NEXT_PAGE, TOGGLE_CONTROLS }
