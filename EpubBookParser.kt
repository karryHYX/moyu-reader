package com.moyu.reader.parser

import com.moyu.reader.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import com.moyu.reader.util.readUpTo

class EpubBookParser : BookParser {
    override val format = BookFormat.EPUB

    override suspend fun canParse(file: File, originalFileName: String): Boolean = withContext(Dispatchers.IO) {
        if (!originalFileName.substringAfterLast('.', "").equals("epub", true)) return@withContext false
        runCatching {
            ZipFile(file).use { zip ->
                zip.getEntry("mimetype")?.let { entry ->
                    zip.getInputStream(entry).bufferedReader().use { it.readText().trim() == "application/epub+zip" }
                } ?: (zip.getEntry("META-INF/container.xml") != null)
            }
        }.getOrDefault(false)
    }

    override suspend fun parse(request: ParseRequest): ParsedBook = withContext(Dispatchers.IO) {
        try {
            ZipFile(request.file).use { zip ->
                val opfPath = findOpfPath(zip)
                val opf = parseXml(readEntry(zip, opfPath, XML_LIMIT))
                val base = opfPath.substringBeforeLast('/', "")
                val manifest = opf.localElements("item").associate { element ->
                    element.attr("id") to ManifestItem(
                        href = resolve(base, element.attr("href")),
                        mediaType = element.attr("media-type"),
                        properties = element.attr("properties"),
                    )
                }
                val spineIds = opf.localElements("itemref").map { it.attr("idref") }.filter { it.isNotBlank() }
                val readingOrder = spineIds.mapNotNull(manifest::get)
                    .filter { it.mediaType.contains("html", true) || it.mediaType.contains("xhtml", true) }
                if (readingOrder.isEmpty()) throw ParserException("EPUB 没有可阅读的正文顺序")

                val navTitles = parseNavigationTitles(zip, manifest)
                var totalCharacters = 0L
                val chapters = readingOrder.mapIndexed { index, item ->
                    val content = htmlToText(readEntry(zip, item.href, HTML_LIMIT))
                    totalCharacters += content.length
                    val heading = navTitles[item.href]
                        ?: extractHeading(readEntry(zip, item.href, HTML_LIMIT))
                        ?: "第 ${index + 1} 章"
                    ParsedChapter(
                        index = index,
                        title = heading.take(80),
                        locator = item.href,
                        characterCount = content.length.toLong(),
                    )
                }

                val title = opf.firstLocalText("title")
                    ?.takeIf { it.isNotBlank() }
                    ?: request.originalFileName.substringBeforeLast('.')
                val author = opf.firstLocalText("creator")?.takeIf { it.isNotBlank() } ?: "未知作者"
                val coverItem = manifest.values.firstOrNull { "cover-image" in it.properties }
                    ?: opf.localElements("meta")
                        .firstOrNull { it.attr("name").equals("cover", true) }
                        ?.attr("content")
                        ?.let(manifest::get)
                val coverBytes = coverItem?.let { runCatching { readEntry(zip, it.href, COVER_LIMIT) }.getOrNull() }
                val coverExt = coverItem?.href?.substringAfterLast('.', "jpg")?.lowercase()?.takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
                ParsedBook(
                    metadata = ParsedMetadata(
                        title = title,
                        author = author,
                        format = format,
                        charset = null,
                        totalCharacters = totalCharacters,
                        coverBytes = coverBytes,
                        coverExtension = coverExt,
                    ),
                    chapters = chapters,
                )
            }
        } catch (error: ParserException) {
            throw error
        } catch (error: Exception) {
            throw ParserException("EPUB 文件已损坏或结构不完整", error)
        }
    }

    override suspend fun readChapter(file: File, chapter: ParsedChapter, charset: String?): String = withContext(Dispatchers.IO) {
        try {
            ZipFile(file).use { zip -> htmlToText(readEntry(zip, chapter.locator, HTML_LIMIT)).withoutRepeatedHeading(chapter.title) }
        } catch (error: Exception) {
            throw ParserException("这一章的内容已损坏", error)
        }
    }

    private fun findOpfPath(zip: ZipFile): String {
        val container = zip.getEntry("META-INF/container.xml")
            ?: throw ParserException("EPUB 缺少 container.xml")
        val document = parseXml(zip.getInputStream(container).use { it.readUpTo(XML_LIMIT) })
        return document.localElements("rootfile").firstOrNull()?.attr("full-path")
            ?.takeIf { it.isNotBlank() }
            ?: throw ParserException("EPUB 没有声明内容包")
    }

    private fun parseNavigationTitles(zip: ZipFile, manifest: Map<String, ManifestItem>): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val nav = manifest.values.firstOrNull { "nav" in it.properties }
        if (nav != null) {
            runCatching {
                val base = nav.href.substringBeforeLast('/', "")
                val doc = Jsoup.parse(readEntry(zip, nav.href, HTML_LIMIT).inputStream(), null, "")
                doc.select("nav a[href], ol a[href]").forEach { link ->
                    val href = link.attr("href").substringBefore('#')
                    result[resolve(base, href)] = link.text().trim()
                }
            }
        }
        val ncx = manifest.values.firstOrNull { it.mediaType.contains("ncx", true) }
        if (ncx != null) {
            runCatching {
                val base = ncx.href.substringBeforeLast('/', "")
                val doc = parseXml(readEntry(zip, ncx.href, XML_LIMIT))
                doc.localElements("navPoint").forEach { point ->
                    val src = point.getAllElements().firstOrNull { it.tagName().substringAfter(':') == "content" }?.attr("src")?.substringBefore('#')
                    val label = point.getAllElements().firstOrNull { it.tagName().substringAfter(':') == "text" }?.text()
                    if (!src.isNullOrBlank() && !label.isNullOrBlank()) result.putIfAbsent(resolve(base, src), label.trim())
                }
            }
        }
        return result
    }

    private fun extractHeading(bytes: ByteArray): String? {
        val doc = Jsoup.parse(bytes.inputStream(), null, "")
        return doc.selectFirst("h1, h2, h3, title")?.text()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun htmlToText(bytes: ByteArray): String {
        val doc = Jsoup.parse(bytes.inputStream(), null, "")
        doc.select("script,style,svg,noscript").remove()
        val blocks = doc.select("h1,h2,h3,h4,p,li,blockquote,pre")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
        return (if (blocks.isNotEmpty()) blocks.joinToString("\n\n") else doc.body().text())
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun readEntry(zip: ZipFile, path: String, limit: Int): ByteArray {
        val clean = path.replace('\\', '/').removePrefix("/")
        val entry = zip.getEntry(clean) ?: throw ParserException("EPUB 缺少资源：$clean")
        if (entry.size > limit) throw ParserException("EPUB 单个资源过大：$clean")
        return zip.getInputStream(entry).use { input ->
            val bytes = input.readUpTo(limit + 1)
            if (bytes.size > limit) throw ParserException("EPUB 单个资源过大：$clean")
            bytes
        }
    }

    private fun parseXml(bytes: ByteArray): Document = Jsoup.parse(
        String(bytes, StandardCharsets.UTF_8),
        "",
        Parser.xmlParser(),
    )

    private fun Document.localElements(name: String) = getAllElements().filter { it.tagName().substringAfter(':').equals(name, true) }
    private fun Document.firstLocalText(name: String) = localElements(name).firstOrNull()?.text()?.trim()

    private fun resolve(base: String, href: String): String {
        val decoded = URLDecoder.decode(href.substringBefore('#'), StandardCharsets.UTF_8.name()).replace('\\', '/')
        val stack = ArrayDeque<String>()
        (if (base.isBlank()) decoded else "$base/$decoded").split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }

    private data class ManifestItem(val href: String, val mediaType: String, val properties: String)

    companion object {
        private const val XML_LIMIT = 2 * 1024 * 1024
        private const val HTML_LIMIT = 16 * 1024 * 1024
        private const val COVER_LIMIT = 10 * 1024 * 1024
    }
}

private fun String.withoutRepeatedHeading(title: String): String {
    val firstBreak = indexOf('\n')
    val first = if (firstBreak >= 0) substring(0, firstBreak) else this
    fun String.normalized() = trim().replace(Regex("[\\t　 ]+"), " ").trimEnd('：', ':')
    return if (first.normalized() == title.normalized()) {
        if (firstBreak >= 0) substring(firstBreak + 1).trimStart('\r', '\n') else ""
    } else this
}
