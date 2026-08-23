package com.moyu.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moyu.reader.data.LibraryRepository
import com.moyu.reader.model.Book
import com.moyu.reader.model.Bookmark
import com.moyu.reader.model.Chapter
import com.moyu.reader.model.ReaderAnchor
import com.moyu.reader.model.SearchHit
import com.moyu.reader.reader.ReadingProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ReaderPanel { NONE, DIRECTORY, PROGRESS, SEARCH, BOOKMARKS, READING_SETTINGS, THEME, FONT, TTS }
enum class PageTurnCommand { PREVIOUS, NEXT }

data class ReaderUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val chapterIndex: Int = 0,
    val text: String = "",
    val characterOffset: Int = 0,
    val loading: Boolean = true,
    val controlsVisible: Boolean = false,
    val panel: ReaderPanel = ReaderPanel.NONE,
    val bookmarks: List<Bookmark> = emptyList(),
    val searchQuery: String = "",
    val searchHits: List<SearchHit> = emptyList(),
    val searching: Boolean = false,
    val autoPageIntervalSeconds: Int = 0,
    val errorMessage: String? = null,
    val transientMessage: String? = null,
) {
    val currentChapter: Chapter? get() = chapters.getOrNull(chapterIndex)
}

class ReaderViewModel(
    private val bookId: String,
    private val repository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()
    private val _pageTurns = MutableSharedFlow<PageTurnCommand>(
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val pageTurns: SharedFlow<PageTurnCommand> = _pageTurns.asSharedFlow()
    private val sessionStartedAt = System.currentTimeMillis()
    private var firstAnchor = 0
    private var saveJob: Job? = null
    private var searchJob: Job? = null
    private var messageJob: Job? = null

    init {
        viewModelScope.launch {
            val book = repository.getBook(bookId)
            val chapters = repository.getChapters(bookId)
            val index = book?.currentChapter?.coerceIn(0, (chapters.size - 1).coerceAtLeast(0)) ?: 0
            firstAnchor = book?.currentPosition ?: 0
            _state.value = _state.value.copy(book = book, chapters = chapters, chapterIndex = index, characterOffset = firstAnchor)
            loadChapter(index, firstAnchor)
        }
        viewModelScope.launch {
            repository.observeBookmarks(bookId).collectLatest { list -> _state.value = _state.value.copy(bookmarks = list) }
        }
    }

    fun toggleControls() { _state.value = _state.value.copy(controlsVisible = !_state.value.controlsVisible) }
    fun hideControls() { _state.value = _state.value.copy(controlsVisible = false) }
    fun showPanel(panel: ReaderPanel) { _state.value = _state.value.copy(panel = panel, controlsVisible = true) }
    fun dismissPanel() { _state.value = _state.value.copy(panel = ReaderPanel.NONE) }

    fun previousChapter() = goToChapter(_state.value.chapterIndex - 1)
    fun nextChapter() = goToChapter(_state.value.chapterIndex + 1)
    fun previousChapterAtEnd() = goToChapter(_state.value.chapterIndex - 1, Int.MAX_VALUE)
    fun retryChapter() = goToChapter(_state.value.chapterIndex, _state.value.characterOffset)
    fun requestPageTurn(command: PageTurnCommand) { _pageTurns.tryEmit(command) }
    fun setAutoPageInterval(seconds: Int) {
        _state.value = _state.value.copy(autoPageIntervalSeconds = seconds.coerceIn(0, 60))
    }
    fun goToChapter(index: Int, offset: Int = 0) {
        if (index !in _state.value.chapters.indices) return
        viewModelScope.launch { loadChapter(index, offset); dismissPanel() }
    }

    fun updateAnchor(offset: Int) {
        val state = _state.value
        val safeOffset = offset.coerceIn(0, state.text.length)
        _state.value = state.copy(characterOffset = safeOffset)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            persistAnchor()
        }
    }

    fun addBookmark() {
        val state = _state.value
        val chapter = state.currentChapter ?: return
        val start = (state.characterOffset - 30).coerceAtLeast(0)
        val end = (state.characterOffset + 90).coerceAtMost(state.text.length)
        val excerpt = state.text.substring(start, end).replace('\n', ' ')
        viewModelScope.launch {
            repository.addBookmark(bookId, chapter.id, state.characterOffset, excerpt)
            showMessage("书签已保存")
        }
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch { repository.deleteBookmark(id) }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchHits = emptyList(), searching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(180)
            _state.value = _state.value.copy(searching = true)
            val hits = repository.searchBook(bookId, query)
            if (_state.value.searchQuery == query) {
                _state.value = _state.value.copy(searchHits = hits, searching = false)
            }
        }
    }

    fun openSearchHit(hit: SearchHit) {
        val index = _state.value.chapters.indexOfFirst { it.id == hit.chapterId }
        if (index >= 0) goToChapter(index, hit.characterOffset)
    }

    private suspend fun loadChapter(index: Int, offset: Int) {
        val chapter = _state.value.chapters.getOrNull(index) ?: run {
            _state.value = _state.value.copy(loading = false)
            return
        }
        _state.value = _state.value.copy(loading = true, errorMessage = null, chapterIndex = index, characterOffset = offset)
        runCatching { repository.readChapter(bookId, chapter.id) }
            .onSuccess { text ->
                _state.value = _state.value.copy(text = text, characterOffset = offset.coerceIn(0, text.length), loading = false)
                persistAnchor()
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    errorMessage = error.message ?: "章节载入失败，请重试",
                )
            }
    }

    private fun showMessage(message: String) {
        messageJob?.cancel()
        _state.value = _state.value.copy(transientMessage = message)
        messageJob = viewModelScope.launch {
            delay(1_800)
            _state.value = _state.value.copy(transientMessage = null)
        }
    }

    private suspend fun persistAnchor() {
        val state = _state.value
        val chapter = state.currentChapter ?: return
        val progress = ReadingProgress.calculate(state.chapterIndex, state.chapters.size, state.characterOffset, state.text.length)
        repository.saveAnchor(ReaderAnchor(bookId, chapter.id, state.characterOffset, progress), state.chapterIndex)
    }

    override fun onCleared() {
        val characters = kotlin.math.abs(_state.value.characterOffset - firstAnchor).toLong()
        viewModelScope.launch { repository.recordReadingSession(bookId, sessionStartedAt, characters) }
        super.onCleared()
    }
}
