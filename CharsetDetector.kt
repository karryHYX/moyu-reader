package com.moyu.reader.parser

import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import kotlin.math.max
import com.moyu.reader.util.readUpTo

data class DetectedCharset(
    val name: String,
    val confidence: Float,
    val bomLength: Int,
)

class CharsetDetector {
    fun detect(file: File, override: String? = null): DetectedCharset {
        if (override != null) {
            val charset = Charset.forName(override)
            return DetectedCharset(charset.name(), 1f, bomLength(file))
        }
        val sample = file.inputStream().buffered().use { it.readUpTo(SAMPLE_SIZE) }
        detectBom(sample)?.let { return it }

        val detector = UniversalDetector(null)
        detector.handleData(sample, 0, sample.size)
        detector.dataEnd()
        val universal = detector.detectedCharset
        detector.reset()

        val candidates = linkedSetOf<String>()
        universal?.let(candidates::add)
        candidates += listOf("UTF-8", "GB18030", "GBK", "Big5", "UTF-16LE", "UTF-16BE")
        val scored = candidates.mapNotNull { name ->
            runCatching {
                val charset = Charset.forName(normalize(name))
                charset.name() to score(sample, charset, universal)
            }.getOrNull()
        }.maxByOrNull { it.second }
        return DetectedCharset(scored?.first ?: "UTF-8", scored?.second ?: .45f, 0)
    }

    private fun detectBom(bytes: ByteArray): DetectedCharset? = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
            DetectedCharset("UTF-8", 1f, 3)
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
            DetectedCharset("UTF-16LE", 1f, 2)
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
            DetectedCharset("UTF-16BE", 1f, 2)
        else -> null
    }

    private fun bomLength(file: File): Int = file.inputStream().use { input ->
        detectBom(input.readUpTo(3))?.bomLength ?: 0
    }

    private fun normalize(value: String): String = when (value.uppercase()) {
        "GB2312", "GB_2312-80", "HZ-GB-2312" -> "GB18030"
        "BIG-5" -> "Big5"
        else -> value
    }

    private fun score(bytes: ByteArray, charset: Charset, universal: String?): Float {
        val decoded = runCatching {
            charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrElse {
            charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }
        if (decoded.isEmpty()) return .1f
        val replacements = decoded.count { it == '\uFFFD' }
        val controls = decoded.count { it.code in 0..8 || it.code in 14..31 }
        val readable = decoded.count { it.isLetterOrDigit() || it in COMMON_PUNCTUATION || it.isWhitespace() }
        val chinese = decoded.count { it.code in 0x3400..0x9FFF }
        var value = readable.toFloat() / decoded.length
        value -= replacements.toFloat() / max(1, decoded.length) * 4f
        value -= controls.toFloat() / max(1, decoded.length) * 2f
        value += (chinese.toFloat() / max(1, decoded.length)).coerceAtMost(.25f)
        if (universal != null && normalize(universal).equals(charset.name(), ignoreCase = true)) value += .12f
        if (charset.name().equals("UTF-8", true) && replacements == 0) value += .08f
        return value.coerceIn(0f, 1f)
    }

    companion object {
        private const val SAMPLE_SIZE = 256 * 1024
        private val COMMON_PUNCTUATION = setOf('，', '。', '！', '？', '；', '：', '“', '”', '‘', '’', '—', '…', '、', ',', '.', '!', '?', ':', ';')
    }
}
