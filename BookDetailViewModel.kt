package com.moyu.reader.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moyu.reader.data.LibraryRepository
import com.moyu.reader.model.Book
import com.moyu.reader.model.Bookmark
import com.moyu.reader.model.Chapter
import com.moyu.reader.model.ReaderAnchor
import com.moyu.reader.model.SearchHit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val reparsing: Boolean = false,
    val message: String? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val searchQuery: String = "",
    val searchHits: List<SearchHit> = emptyList(),
    val searching: Boolean = false,
)

private data class DetailSearchState(
    val query: String = "",
    val hits: List<SearchHit> = emptyList(),
    val loading: Boolean = false,
)

class BookDetailViewModel(
    private val bookId: String,
    private val repository: LibraryRepository,
) : ViewModel() {
    private val operation = kotlinx.coroutines.flow.MutableStateFlow(false to null as String?)
    private val searchState = MutableStateFlow(DetailSearchState())
    private var searchJob: Job? = null
    val state: StateFlow<BookDetailUiState> = combine(
        repository.observeBook(bookId),
        repository.observeChapters(bookId),
        repository.observeBookmarks(bookId),
        operation,
        searchState,
    ) { book, chapters, bookmarks, op, search ->
        BookDetailUiState(
            book = book,
            chapters = chapters,
            reparsing = op.first,
            message = op.second,
            bookmarks = bookmarks,
            searchQuery = search.query,
            searchHits = search.hits,
            searching = search.loading,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailUiState())

    fun toggleFavorite() = viewModelScope.launch { repository.toggleFavorite(bookId) }
    fun updateMetadata(title: String, author: String) = viewModelScope.launch {
        repository.updateBookMetadata(bookId, title, author)
        operation.value = false to "书籍信息已保存"
    }
    fun markFinished() = viewModelScope.launch {
        repository.markBookFinished(bookId)
        operation.value = false to "已标记为读完"
    }
    fun resetProgress() = viewModelScope.launch {
        repository.resetBookProgress(bookId)
        operation.value = false to "阅读进度已重置"
    }
    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        repository.delete(setOf(bookId))
        onDeleted()
    }
    fun reparse(charset: String?) = viewModelScope.launch {
        operation.value = true to null
        operation.value = runCatching { repository.reparseBook(bookId, charset) }
            .fold({ false to "重新解析完成" }, { false to (it.message ?: "重新解析没有完成") })
    }

    fun search(query: String) {
        searchState.value = searchState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            searchState.value = DetailSearchState(query = query)
            return
        }
        searchJob = viewModelScope.launch {
            delay(180)
            searchState.value = searchState.value.copy(loading = true)
            val hits = repository.searchBook(bookId, query)
            if (searchState.value.query == query) {
                searchState.value = DetailSearchState(query = query, hits = hits, loading = false)
            }
        }
    }

    fun openSearchHit(hit: SearchHit, onOpenReader: () -> Unit) = viewModelScope.launch {
        val chapters = repository.getChapters(bookId)
        val index = chapters.indexOfFirst { it.id == hit.chapterId }
        if (index >= 0) openLocation(index, hit.characterOffset, onOpenReader)
    }

    fun openBookmark(bookmark: Bookmark, onOpenReader: () -> Unit) = viewModelScope.launch {
        val chapters = repository.getChapters(bookId)
        val index = chapters.indexOfFirst { it.id == bookmark.chapterId }
        if (index >= 0) openLocation(index, bookmark.characterOffset, onOpenReader)
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch { repository.deleteBookmark(id) }

    fun openLocation(chapterIndex: Int, requestedOffset: Int, onOpenReader: () -> Unit) = viewModelScope.launch {
        val chapters = repository.getChapters(bookId)
        val chapter = chapters.getOrNull(chapterIndex) ?: return@launch
        val text = runCatching { repository.readChapter(bookId, chapter.id) }.getOrElse { "" }
        val offset = requestedOffset.coerceIn(0, text.length)
        val local = if (text.isEmpty()) 0f else offset.toFloat() / text.length
        val progress = (chapterIndex + local) / chapters.size.coerceAtLeast(1)
        repository.saveAnchor(ReaderAnchor(bookId, chapter.id, offset, progress), chapterIndex)
        onOpenReader()
    }
}
