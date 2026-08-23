@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moyu.reader.ui.library

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moyu.reader.model.Book
import com.moyu.reader.model.LibraryFilter
import com.moyu.reader.model.LibraryLayout
import com.moyu.reader.model.LibrarySort
import com.moyu.reader.ui.designsystem.EditorialSectionTitle
import com.moyu.reader.ui.designsystem.GeneratedBookCover
import com.moyu.reader.ui.designsystem.LocalMoyuColors
import com.moyu.reader.ui.designsystem.MoyuGlyph
import com.moyu.reader.ui.designsystem.MoyuGlyphIcon
import com.moyu.reader.ui.designsystem.MoyuPrimaryButton
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (String) -> Unit,
    onContinueReading: (String) -> Unit,
    onImport: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchVisible by remember { mutableStateOf(false) }
    var sortVisible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        if (state.selected.isNotEmpty()) {
            SelectionBar(state.selected.size, viewModel::clearSelection, viewModel::deleteSelected)
        } else {
            LibraryHeader(
                layout = state.layout,
                searchVisible = searchVisible,
                query = state.query,
                onQuery = viewModel::setQuery,
                onToggleSearch = { searchVisible = !searchVisible; if (!searchVisible) viewModel.setQuery("") },
                onToggleLayout = { viewModel.setLayout(if (state.layout == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID) },
                onSort = { sortVisible = true },
                filter = state.filter,
                onFilter = viewModel::setFilter,
            )
        }
        if (state.loading) {
            LoadingLibrary()
        } else if (state.books.isEmpty()) {
            EmptyLibrary(searching = state.query.isNotBlank() || state.filter != LibraryFilter.ALL, onImport = onImport)
        } else if (state.layout == LibraryLayout.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(104.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(state.books, key = { it.id }) { book ->
                    BookGridItem(
                        book = book,
                        selected = book.id in state.selected,
                        selectionMode = state.selected.isNotEmpty(),
                        onClick = { if (state.selected.isNotEmpty()) viewModel.toggleSelection(book.id) else onOpenBook(book.id) },
                        onLongClick = { viewModel.toggleSelection(book.id) },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                items(state.books, key = { it.id }) { book ->
                    BookListItem(
                        book = book,
                        selected = book.id in state.selected,
                        onClick = { if (state.selected.isNotEmpty()) viewModel.toggleSelection(book.id) else onOpenBook(book.id) },
                        onLongClick = { viewModel.toggleSelection(book.id) },
                    )
                }
            }
        }
    }
    if (sortVisible) {
        ModalBottomSheet(onDismissRequest = { sortVisible = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialSectionTitle("ORDER", "排序与筛选")
                Text("书架顶部的分类会立即生效；在这里选择当前列表的排列顺序。", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
                Spacer(Modifier.height(6.dp))
                listOf(
                    LibrarySort.RECENT to "最近阅读",
                    LibrarySort.ADDED to "添加时间",
                    LibrarySort.TITLE to "书名",
                    LibrarySort.AUTHOR to "作者",
                    LibrarySort.PROGRESS to "阅读进度",
                ).forEach { (sort, label) ->
                    val selected = state.sort == sort
                    Surface(
                        onClick = { viewModel.setSort(sort); sortVisible = false },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.divider),
                    ) {
                        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            Text(if (selected) "已选" else "›", color = if (selected) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.textTertiary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingLibrary() {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(3) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 54.dp, height = 76.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth(.55f).height(12.dp).background(LocalMoyuColors.current.divider))
                    Box(Modifier.fillMaxWidth(.32f).height(8.dp).background(LocalMoyuColors.current.divider.copy(alpha = .7f)))
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    layout: LibraryLayout,
    searchVisible: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onToggleLayout: () -> Unit,
    onSort: () -> Unit,
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("LIBRARY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("我的书架", style = MaterialTheme.typography.headlineLarge)
            }
            GlyphButton(MoyuGlyph.SEARCH, "搜索书架", onToggleSearch)
            GlyphButton(if (layout == LibraryLayout.GRID) MoyuGlyph.LIST else MoyuGlyph.GRID, "切换布局", onToggleLayout)
            LibrarySortButton(onSort)
        }
        Spacer(Modifier.height(13.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LibraryFilter.entries.forEach { item ->
                val selected = filter == item
                Surface(
                    onClick = { onFilter(item) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(if (selected) 1.dp else .5.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .55f) else LocalMoyuColors.current.divider.copy(alpha = .6f)),
                ) {
                    Text(
                        libraryFilterName(item),
                        Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else LocalMoyuColors.current.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
        if (searchVisible) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().height(50.dp).border(1.dp, if (query.isNotBlank()) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.divider, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MoyuGlyphIcon(MoyuGlyph.SEARCH, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    singleLine = true,
                    decorationBox = { inner -> if (query.isEmpty()) Text("搜索书名或作者", color = LocalMoyuColors.current.textTertiary) else Unit; inner() },
                )
            }
        }
    }
}

@Composable
private fun LibrarySortButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalMoyuColors.current.divider),
        modifier = Modifier.size(42.dp).padding(start = 3.dp).semantics { contentDescription = "排序与筛选" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            MoyuGlyphIcon(MoyuGlyph.SORT, Modifier.size(19.dp), color = LocalMoyuColors.current.textSecondary)
        }
    }
}

private fun libraryFilterName(filter: LibraryFilter) = when (filter) {
    LibraryFilter.ALL -> "全部"
    LibraryFilter.UNREAD -> "未读"
    LibraryFilter.READING -> "在读"
    LibraryFilter.FINISHED -> "已读"
    LibraryFilter.FAVORITE -> "收藏"
}

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton(MoyuGlyph.BACK, "退出选择", onClose)
        Text("已选择 $count 本", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Surface(onClick = onDelete, color = Color.Transparent) { Text("删除", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun GlyphButton(glyph: MoyuGlyph, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(43.dp).semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.Center) { MoyuGlyphIcon(glyph, Modifier.size(22.dp)) }
    }
}

@Composable
private fun BookGridItem(book: Book, selected: Boolean, selectionMode: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(.68f).then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary) else Modifier)) {
            BookCover(book, Modifier.fillMaxSize())
            if (selected) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(8.dp))
        Text(book.title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${(book.readingProgress * 100).toInt()}% · ${book.author}", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BookListItem(book: Book, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f) else Color.Transparent)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book, Modifier.size(width = 54.dp, height = 76.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(book.author, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textSecondary)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(LocalMoyuColors.current.divider)) {
                Box(Modifier.fillMaxWidth(book.readingProgress.coerceIn(0f, 1f)).height(2.dp).background(MaterialTheme.colorScheme.primary))
            }
        }
        Text(book.lastReadAt?.let { DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it)) } ?: "未读", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
    }
}

@Composable
private fun BookCover(book: Book, modifier: Modifier) {
    val bitmap = remember(book.coverPath) {
        book.coverPath?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    if (bitmap != null) Image(bitmap, book.title, modifier.clip(RoundedCornerShape(1.dp)), contentScale = ContentScale.Crop)
    else GeneratedBookCover(book.title, book.author, book.id, modifier, book.readingProgress)
}

@Composable
private fun EmptyLibrary(searching: Boolean, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(34.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 96.dp, height = 112.dp).border(1.dp, LocalMoyuColors.current.divider)) {
            Box(Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 24.dp).width(50.dp).height(2.dp).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.height(24.dp))
        Text(if (searching) "没有找到这本书" else "书架还是空的", style = MaterialTheme.typography.headlineMedium)
        Text(if (searching) "换一个书名或作者试试。" else "导入 TXT 或 EPUB，建立你的本地书库。", color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 9.dp, bottom = 24.dp))
        if (!searching) MoyuPrimaryButton("导入第一本书", onImport, Modifier.fillMaxWidth())
    }
}
