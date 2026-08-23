package com.moyu.reader.reader

/**
 * Normalises indentation only for rendering while preserving the exact string length.
 * Saved anchors and search offsets therefore continue to point at the original source.
 */
object ReaderTextFormatter {
    private const val ZERO_WIDTH_SPACE = '\u200B'

    fun forDisplay(source: String): String {
        if (source.isEmpty()) return source
        val output = source.toCharArray()
        var atLineStart = true
        output.indices.forEach { index ->
            val character = output[index]
            when {
                character == '\n' -> atLineStart = true
                atLineStart && character.isIndentWhitespace() -> output[index] = ZERO_WIDTH_SPACE
                else -> atLineStart = false
            }
        }
        return String(output)
    }

    fun isParagraphStart(text: String, index: Int): Boolean =
        index == 0 || (index in 1..text.length && text[index - 1] == '\n')

    private fun Char.isIndentWhitespace(): Boolean =
        this == ' ' || this == '\t' || this == '\u3000' || this == '\u00A0'
}
