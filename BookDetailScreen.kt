@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moyu.reader.ui.book

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moyu.reader.model.Book
import com.moyu.reader.model.Bookmark
import com.moyu.reader.model.Chapter
import com.moyu.reader.model.SearchHit
import com.moyu.reader.ui.designsystem.GeneratedBookCover
import com.moyu.reader.ui.designsystem.LocalMoyuColors
import com.moyu.reader.ui.designsystem.MoyuGlyph
import com.moyu.reader.ui.designsystem.MoyuGlyphIcon
import com.moyu.reader.ui.designsystem.MoyuPrimaryButton

@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onBack: () -> Unit,
    onRead: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val book = state.book
    var reparseSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    var activePanel by remember { androidx.compose.runtime.mutableStateOf(DetailPanel.NONE) }
    var managementVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    var editorVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    var deleteConfirmVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            DetailGlyphButton(MoyuGlyph.BACK, "返回", onBack)
            Text("书籍详情", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            DetailGlyphButton(MoyuGlyph.BOOKMARK, if (book?.favorite == true) "取消收藏" else "收藏", viewModel::toggleFavorite, active = book?.favorite == true)
            DetailGlyphButton(MoyuGlyph.MORE, "书籍管理", onClick = { managementVisible = true })
        }
        if (book == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("正在打开书籍…", color = LocalMoyuColors.current.textSecondary) }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DetailCover(book, Modifier.size(width = 126.dp, height = 184.dp))
                    Column(Modifier.weight(1f).padding(start = 22.dp)) {
                        Text("${book.format.name} · ${book.charset ?: "EPUB"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(book.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 10.dp))
                        Text(book.author, style = MaterialTheme.typography.bodyMedium, color = LocalMoyuColors.current.textSecondary)
                        Spacer(Modifier.height(18.dp))
                        Text("${(book.readingProgress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        Box(Modifier.fillMaxWidth().height(2.dp).background(LocalMoyuColors.current.divider)) {
                            Box(Modifier.fillMaxWidth(book.readingProgress.coerceIn(0f, 1f)).height(2.dp).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("${(book.readingProgress * 100).toInt()}%", "阅读进度")
                    Stat(formatCharacters(book.totalCharacters), "字数")
                    Stat(book.chapterCount.toString(), "章节")
                }
                Spacer(Modifier.height(28.dp))
                MoyuPrimaryButton(if (book.readingProgress > 0f) "继续阅读" else "开始阅读", onRead, Modifier.fillMaxWidth())
                Spacer(Modifier.height(26.dp))
                DetailRow("目录", "${state.chapters.size} 章") { activePanel = DetailPanel.DIRECTORY }
                DetailRow("全文搜索", if (book.searchIndexed) "索引已完成" else "正在准备") { activePanel = DetailPanel.SEARCH }
                DetailRow("书签", if (state.bookmarks.isEmpty()) "暂无书签" else "${state.bookmarks.size} 条") { activePanel = DetailPanel.BOOKMARKS }
                DetailRow("重新解析", if (state.reparsing) "正在处理" else "编码与章节识别") { reparseSheet = true }
                state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }
                DetailRow("文件", "${book.originalFileName} · ${formatBytes(book.fileSize)}", {})
            }
        }
    }
    if (reparseSheet) {
        ModalBottomSheet(onDismissRequest = { reparseSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 30.dp)) {
                Text("重新解析", style = MaterialTheme.typography.headlineMedium)
                Text("重新识别会重建目录与全文索引，并清空现有书签。", color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                listOf(null to "自动识别", "UTF-8" to "UTF-8", "GB18030" to "GB18030", "GBK" to "GBK", "Big5" to "Big5", "UTF-16LE" to "UTF-16 LE").forEach { (charset, label) ->
                    Surface(onClick = { reparseSheet = false; viewModel.reparse(charset) }, color = Color.Transparent) {
                        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Text("›", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    when (activePanel) {
        DetailPanel.DIRECTORY -> DetailSheet(onDismiss = { activePanel = DetailPanel.NONE }) {
            DetailDirectoryPanel(
                chapters = state.chapters,
                currentChapter = book?.currentChapter ?: 0,
                onOpen = { chapter -> viewModel.openLocation(chapter.index, 0, onRead) },
            )
        }
        DetailPanel.SEARCH -> DetailSheet(onDismiss = { activePanel = DetailPanel.NONE }) {
            DetailSearchPanel(
                indexed = book?.searchIndexed == true,
                query = state.searchQuery,
                hits = state.searchHits,
                searching = state.searching,
                onQuery = viewModel::search,
                onOpen = { hit -> viewModel.openSearchHit(hit, onRead) },
            )
        }
        DetailPanel.BOOKMARKS -> DetailSheet(onDismiss = { activePanel = DetailPanel.NONE }) {
            DetailBookmarksPanel(
                bookmarks = state.bookmarks,
                chapters = state.chapters,
                onOpen = { bookmark -> viewModel.openBookmark(bookmark, onRead) },
                onDelete = viewModel::deleteBookmark,
            )
        }
        DetailPanel.NONE -> Unit
    }
    if (managementVisible && book != null) {
        DetailSheet(onDismiss = { managementVisible = false }) {
            BookManagementPanel(
                completed = book.readingProgress >= .995f,
                onEdit = { managementVisible = false; editorVisible = true },
                onToggleFinished = {
                    managementVisible = false
                    if (book.readingProgress >= .995f) viewModel.resetProgress() else viewModel.markFinished()
                },
                onDelete = { managementVisible = false; deleteConfirmVisible = true },
            )
        }
    }
    if (editorVisible && book != null) {
        BookMetadataEditor(
            book = book,
            onDismiss = { editorVisible = false },
            onSave = { title, author -> editorVisible = false; viewModel.updateMetadata(title, author) },
        )
    }
    if (deleteConfirmVisible && book != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmVisible = false },
            title = { Text("删除书籍") },
            text = { Text("将移除“${book.title}”及其阅读进度、书签和笔记。") },
            confirmButton = { TextButton(onClick = { deleteConfirmVisible = false; viewModel.delete(onBack) }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteConfirmVisible = false }) { Text("保留") } },
        )
    }
}

private enum class DetailPanel { NONE, DIRECTORY, SEARCH, BOOKMARKS }

@Composable
private fun BookManagementPanel(
    completed: Boolean,
    onEdit: () -> Unit,
    onToggleFinished: () -> Unit,
    onDelete: () -> Unit,
) {
    Text("书籍管理", style = MaterialTheme.typography.headlineMedium)
    Text("整理书籍信息与阅读状态", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
    ManagementRow("编辑书名与作者", "修改书架展示信息", onEdit)
    ManagementRow(if (completed) "重置阅读进度" else "标记为已读完", if (completed) "从第一章重新开始" else "进度将显示为 100%", onToggleFinished)
    ManagementRow("删除书籍", "同时清除本地副本、书签与笔记", onDelete, destructive = true)
}

@Composable
private fun ManagementRow(title: String, detail: String, onClick: () -> Unit, destructive: Boolean = false) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(top = 3.dp))
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
}

@Composable
private fun BookMetadataEditor(book: Book, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember(book.id) { androidx.compose.runtime.mutableStateOf(book.title) }
    var author by remember(book.id) { androidx.compose.runtime.mutableStateOf(book.author) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑书籍信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("书名") }, singleLine = true)
                OutlinedTextField(author, { author = it }, label = { Text("作者") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, author) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DetailSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp)) { content() }
    }
}

@Composable
private fun DetailDirectoryPanel(chapters: List<Chapter>, currentChapter: Int, onOpen: (Chapter) -> Unit) {
    Text("目录", style = MaterialTheme.typography.headlineMedium)
    Text("选择章节后从该章开头进入阅读", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
    LazyColumn(Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 560.dp)) {
        items(chapters, key = { it.id }) { chapter ->
            val active = chapter.index == currentChapter
            Surface(
                onClick = { onOpen(chapter) },
                color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${chapter.index + 1}".padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.textTertiary)
                    Text(chapter.title, Modifier.weight(1f).padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                    if (active) Text("当前", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DetailSearchPanel(
    indexed: Boolean,
    query: String,
    hits: List<SearchHit>,
    searching: Boolean,
    onQuery: (String) -> Unit,
    onOpen: (SearchHit) -> Unit,
) {
    Text("全文搜索", style = MaterialTheme.typography.headlineMedium)
    Text(if (indexed) "在当前书籍的正文中查找关键词" else "索引正在准备，稍后输入关键词即可检索", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
    Row(
        Modifier.fillMaxWidth().height(52.dp).border(1.dp, if (query.isNotBlank()) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.divider, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoyuGlyphIcon(MoyuGlyph.SEARCH, Modifier.size(20.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            decorationBox = { inner -> if (query.isBlank()) Text("输入正文关键词", color = LocalMoyuColors.current.textTertiary) else Unit; inner() },
        )
    }
    when {
        searching -> CircularProgressIndicator(Modifier.padding(24.dp).size(24.dp), strokeWidth = 2.dp)
        query.isNotBlank() && hits.isEmpty() -> Text("没有找到匹配内容", color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(vertical = 30.dp))
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
        items(hits, key = { "${it.chapterId}:${it.characterOffset}" }) { hit ->
            Surface(onClick = { onOpen(hit) }, color = Color.Transparent) {
                Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
                    Text(hit.chapterTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(hit.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
        }
    }
}

@Composable
private fun DetailBookmarksPanel(
    bookmarks: List<Bookmark>,
    chapters: List<Chapter>,
    onOpen: (Bookmark) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Text("书签", style = MaterialTheme.typography.headlineMedium)
    Text("${bookmarks.size} 条书签 · 轻点可回到保存位置", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
    if (bookmarks.isEmpty()) {
        Text("阅读页面右上角的书签图标可保存当前位置。", color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(vertical = 32.dp))
    } else {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            items(bookmarks, key = { it.id }) { bookmark ->
                Surface(onClick = { onOpen(bookmark) }, color = Color.Transparent) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.Top) {
                        MoyuGlyphIcon(MoyuGlyph.BOOKMARK, Modifier.size(20.dp), accent = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                            Text(chapters.firstOrNull { it.id == bookmark.chapterId }?.title ?: "已失效章节", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(bookmark.excerpt, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                        Surface(onClick = { onDelete(bookmark.id) }, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(9.dp)) {
                            Text("删除", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = LocalMoyuColors.current.textSecondary)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
            }
        }
    }
}

@Composable
private fun DetailGlyphButton(glyph: MoyuGlyph, description: String, onClick: () -> Unit, active: Boolean = false) {
    Surface(onClick = onClick, color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(42.dp).semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.Center) { MoyuGlyphIcon(glyph, Modifier.size(22.dp), accent = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun DetailCover(book: Book, modifier: Modifier) {
    val bitmap = remember(book.coverPath) { book.coverPath?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() } }
    if (bitmap != null) Image(bitmap, book.title, modifier, contentScale = ContentScale.Crop)
    else GeneratedBookCover(book.title, book.author, book.id, modifier, book.readingProgress)
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
    }
}

@Composable
private fun DetailRow(title: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
            Text("  ›", color = MaterialTheme.colorScheme.primary)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
}

private fun formatCharacters(value: Long): String = when {
    value >= 10_000 -> "%.1f万".format(value / 10_000f)
    else -> value.toString()
}

private fun formatBytes(value: Long): String = when {
    value >= 1024L * 1024 -> "%.1f MB".format(value / 1024f / 1024f)
    value >= 1024 -> "%.1f KB".format(value / 1024f)
    else -> "$value B"
}
