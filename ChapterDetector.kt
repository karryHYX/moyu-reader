package com.moyu.reader.parser

data class ChapterCandidate(
    val title: String,
    val byteOffset: Long,
    val score: Float,
)

class ChapterDetector {
    fun candidate(line: String, byteOffset: Long, previousBlank: Boolean): ChapterCandidate? {
        val text = line.trim().replace(Regex("[\\t　 ]+"), " ")
        if (text.isEmpty() || text.length > 64) return null
        val base = when {
            chineseChapter.matches(text) -> .78f
            chineseVolume.matches(text) -> .72f
            englishChapter.matches(text) -> .74f
            specialTitle.matches(text) -> .68f
            numberedHeading.matches(text) -> .58f
            else -> return null
        }
        var score = base
        if (previousBlank) score += .08f
        if (text.length <= 24) score += .05f
        if (text.endsWith('。') || text.endsWith('！') || text.endsWith('？')) score -= .22f
        if (text.count { it == '，' || it == ',' } > 1) score -= .18f
        return ChapterCandidate(text, byteOffset, score.coerceIn(0f, 1f))
            .takeIf { it.score >= .58f }
    }

    fun select(candidates: List<ChapterCandidate>, fileSize: Long): List<ChapterCandidate> {
        if (candidates.isEmpty()) return emptyList()
        val strong = candidates.filter { it.score >= .64f }
        if (strong.size < 2) return emptyList()
        val minDistance = when {
            fileSize > 50L * 1024 * 1024 -> 512L
            else -> 256L
        }
        val selected = ArrayList<ChapterCandidate>()
        for (candidate in strong) {
            val previous = selected.lastOrNull()
            if (previous == null || candidate.byteOffset - previous.byteOffset >= minDistance) {
                selected += candidate
            } else if (candidate.score > previous.score + .08f) {
                selected[selected.lastIndex] = candidate
            }
        }
        val averageDistance = selected.zipWithNext { a, b -> b.byteOffset - a.byteOffset }
            .average().takeUnless { it.isNaN() } ?: fileSize.toDouble()
        return selected.takeIf { it.size >= 2 && averageDistance >= minDistance } ?: emptyList()
    }

    companion object {
        private val chineseNumber = "[〇零一二三四五六七八九十百千万两\\d]+"
        private val chineseChapter = Regex("^第\\s*$chineseNumber\\s*[章节回部篇集]\\s*(?:[·：:、.．\\-— ]+.{0,32})?$", RegexOption.IGNORE_CASE)
        private val chineseVolume = Regex("^(?:第\\s*$chineseNumber\\s*卷|卷\\s*$chineseNumber|[上中下]卷)\\s*(?:[·：:、.．\\-— ]+.{0,32})?$", RegexOption.IGNORE_CASE)
        private val englishChapter = Regex("^(?:chapter|part|volume|book)\\s+[0-9ivxlcdm]+(?:\\s*[.:：—-]\\s*.{0,32})?$", RegexOption.IGNORE_CASE)
        private val specialTitle = Regex("^(楔子|序章|序|前言|引子|引言|后记|尾声|终章|番外(?:篇|章)?(?:[一二三四五六七八九十\\d]+)?|附录)(?:\\s*[：:—-]\\s*.{0,24})?$")
        private val numberedHeading = Regex("^[0-9一二三四五六七八九十]{1,4}[、.．]\\s*[^。！？!?]{1,30}$")
    }
}
