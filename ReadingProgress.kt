package com.moyu.reader.reader

object ReadingProgress {
    fun calculate(chapterIndex: Int, chapterCount: Int, characterOffset: Int, chapterLength: Int): Float {
        if (chapterCount <= 0) return 0f
        val local = if (chapterLength <= 0) 0f else characterOffset.coerceIn(0, chapterLength).toFloat() / chapterLength
        return ((chapterIndex.coerceIn(0, chapterCount - 1) + local) / chapterCount).coerceIn(0f, 1f)
    }
}

