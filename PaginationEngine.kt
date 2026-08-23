package com.moyu.reader.reader

import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import kotlin.math.max
import kotlin.math.ceil

data class PageSlice(val start: Int, val endExclusive: Int)

data class PaginationSpec(
    val viewportWidthPx: Int,
    val viewportHeightPx: Int,
    val fontSizePx: Float,
    val lineHeightMultiplier: Float,
    val paragraphSpacingPx: Float,
    val typeface: Typeface = Typeface.SERIF,
    val fontWeight: Int = 400,
    val firstLineIndentPx: Float = 0f,
    val pageReservedPx: Int = 0,
    val firstPageReservedPx: Int = 0,
)

class AndroidPaginator {
    fun paginate(text: String, spec: PaginationSpec): List<PageSlice> {
        if (text.isEmpty()) return listOf(PageSlice(0, 0))
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG or TextPaint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = spec.fontSizePx
            typeface = spec.typeface
            isFakeBoldText = spec.fontWeight >= 500
        }
        // Compose receives an absolute lineHeight (`fontSize * multiplier`), while
        // StaticLayout's multiplier is relative to the font metrics. Converting the
        // value here keeps its page measurement aligned with what ReaderPage draws.
        val rawLineHeight = paint.fontMetrics.run { descent - ascent }.coerceAtLeast(1f)
        val targetLineHeight = spec.fontSizePx * spec.lineHeightMultiplier
        val layoutLineHeightMultiplier = (targetLineHeight / rawLineHeight).coerceAtLeast(.1f)
        val estimatedLineHeight = targetLineHeight
        val slices = ArrayList<PageSlice>()
        var start = 0
        while (start < text.length) {
            val availableHeight = (
                spec.viewportHeightPx - spec.pageReservedPx -
                    if (slices.isEmpty()) spec.firstPageReservedPx else 0
                ).coerceAtLeast(1)
            var cursor = start
            var usedHeight = 0f
            var hasVisibleParagraph = false

            while (cursor < text.length) {
                // Newlines are source separators, not visible blank Text nodes. Keep
                // them inside the slice so saved character offsets stay exact.
                while (cursor < text.length && text[cursor] == '\n') cursor++
                if (cursor >= text.length) break

                val paragraphEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
                val paragraphGap = if (hasVisibleParagraph) spec.paragraphSpacingPx else 0f
                val remainingHeight = availableHeight - usedHeight - paragraphGap
                val fit = fitParagraph(
                    text = text,
                    start = cursor,
                    endExclusive = paragraphEnd,
                    maxHeight = remainingHeight,
                    paint = paint,
                    widthPx = spec.viewportWidthPx.coerceAtLeast(1),
                    lineHeightMultiplier = layoutLineHeightMultiplier,
                    estimatedLineHeight = estimatedLineHeight,
                    firstLineIndentPx = if (ReaderTextFormatter.isParagraphStart(text, cursor)) spec.firstLineIndentPx else 0f,
                )

                if (fit.lines.isEmpty()) {
                    // A page that already has text ends before this paragraph. For an
                    // unusually small viewport, always consume at least one source
                    // character so pagination makes forward progress.
                    if (hasVisibleParagraph) break
                    cursor = (cursor + 1).coerceAtMost(paragraphEnd)
                    break
                }

                if (fit.fullyFits) {
                    usedHeight += paragraphGap + fit.lines.last().bottom
                    hasVisibleParagraph = true
                    cursor = paragraphEnd
                    while (cursor < text.length && text[cursor] == '\n') cursor++
                    continue
                }

                // ReaderPage renders each source paragraph/continuation as its own
                // Text composable. Select line ends from that exact fragment, rather
                // than a layout that crosses a paragraph boundary; this removes
                // dropped-looking characters and boundary reflow flashes.
                val endLine = preferredPageEnd(text, fit.lines, cursor) ?: fit.lines.last()
                cursor = endLine.endExclusive
                break
            }

            if (cursor <= start) cursor = (start + 1).coerceAtMost(text.length)
            slices += PageSlice(start, cursor)
            start = cursor
        }
        return slices
    }

    private fun fitParagraph(
        text: String,
        start: Int,
        endExclusive: Int,
        maxHeight: Float,
        paint: TextPaint,
        widthPx: Int,
        lineHeightMultiplier: Float,
        estimatedLineHeight: Float,
        firstLineIndentPx: Float,
    ): ParagraphFit {
        if (start >= endExclusive || maxHeight <= 0f) return ParagraphFit(emptyList(), fullyFits = false)
        val maxLines = max(1, (maxHeight / estimatedLineHeight).toInt())
        val estimatedCharactersPerLine = ceil(widthPx / paint.textSize.coerceAtLeast(1f)).toInt().coerceAtLeast(4)
        var windowEnd = (start + estimatedCharactersPerLine * maxLines * 3).coerceAtMost(endExclusive)

        while (true) {
            val localText = if (firstLineIndentPx > 0f) {
                SpannableString(text.substring(start, windowEnd)).apply {
                    setSpan(
                        LeadingMarginSpan.Standard(firstLineIndentPx.toInt(), 0),
                        0,
                        length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            } else null
            val layout = StaticLayout.Builder.obtain(localText ?: text, if (localText == null) start else 0, if (localText == null) windowEnd else localText.length, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(0f, lineHeightMultiplier)
                .build()
            val lines = buildList {
                for (line in 0 until layout.lineCount) {
                    val bottom = layout.getLineBottom(line).toFloat()
                    if (bottom > maxHeight) break
                    val end = if (localText == null) layout.getLineEnd(line) else start + layout.getLineEnd(line)
                    add(FittingLine(end.coerceAtMost(windowEnd), bottom))
                }
            }
            val wholeWindowFits = lines.size == layout.lineCount
            if (wholeWindowFits && windowEnd < endExclusive) {
                val grown = (windowEnd + estimatedCharactersPerLine * maxLines * 2).coerceAtMost(endExclusive)
                if (grown > windowEnd) {
                    windowEnd = grown
                    continue
                }
            }
            return ParagraphFit(lines, fullyFits = wholeWindowFits && windowEnd == endExclusive)
        }
    }

    private fun preferredPageEnd(text: String, fittingLines: List<FittingLine>, pageStart: Int): FittingLine? {
        if (fittingLines.size < 3) return null
        // At most roughly one sixth of a non-final page may be traded for a
        // sentence ending. Otherwise keep the last fitting line and avoid a
        // visibly hollow page.
        val firstPreferredLine = ((fittingLines.size - 1) * .84f).toInt()
        for (line in fittingLines.lastIndex downTo firstPreferredLine) {
            val candidate = fittingLines[line]
            if (candidate.endExclusive > pageStart && endsSentence(text, candidate.endExclusive)) return candidate
        }
        return null
    }

    private fun endsSentence(text: String, exclusiveEnd: Int): Boolean {
        var index = exclusiveEnd - 1
        while (index >= 0 && (text[index].isWhitespace() || text[index] == '\u200B' || text[index] in SENTENCE_CLOSERS)) index--
        return index >= 0 && text[index] in SENTENCE_ENDINGS
    }

    private companion object {
        const val SENTENCE_ENDINGS = "。！？；…!?;"
        const val SENTENCE_CLOSERS = "”’\"』》）】〉〕"
    }

    private data class FittingLine(val endExclusive: Int, val bottom: Float)
    private data class ParagraphFit(val lines: List<FittingLine>, val fullyFits: Boolean)
}

/** Pure pagination boundary loop used by unit tests and non-Android callers. */
class PageBreakCalculator {
    fun calculate(length: Int, fittingEnd: (start: Int) -> Int): List<PageSlice> {
        if (length <= 0) return listOf(PageSlice(0, 0))
        val result = ArrayList<PageSlice>()
        var start = 0
        while (start < length) {
            val proposed = fittingEnd(start).coerceIn(start + 1, length)
            result += PageSlice(start, proposed)
            start = proposed
        }
        return result
    }
}
