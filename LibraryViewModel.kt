package com.moyu.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moyu.reader.data.LibraryRepository
import com.moyu.reader.data.preferences.SettingsRepository
import com.moyu.reader.model.Book
import com.moyu.reader.model.LibraryLayout
import com.moyu.reader.model.LibraryFilter
import com.moyu.reader.model.LibrarySort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val query: String = "",
    val layout: LibraryLayout = LibraryLayout.GRID,
    val sort: LibrarySort = LibrarySort.RECENT,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val selected: Set<String> = emptySet(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: LibraryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val source = query.flatMapLatest { value ->
        if (value.isBlank()) repository.observeBooks() else repository.searchLibrary(value)
    }

    val state: StateFlow<LibraryUiState> = combine(
        source,
        query,
        filter,
        settings.preferences,
        selected,
    ) { books, queryValue, filterValue, preferences, selection ->
        LibraryUiState(
            books = books.filtered(filterValue).sorted(preferences.librarySort),
            query = queryValue,
            layout = preferences.libraryLayout,
            sort = preferences.librarySort,
            filter = filterValue,
            selected = selection,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: LibraryFilter) { filter.value = value }
    fun setLayout(value: LibraryLayout) = viewModelScope.launch { settings.setLibraryLayout(value) }
    fun setSort(value: LibrarySort) = viewModelScope.launch { settings.setLibrarySort(value) }
    fun toggleSelection(id: String) { selected.value = selected.value.toMutableSet().apply { if (!add(id)) remove(id) } }
    fun clearSelection() { selected.value = emptySet() }
    fun deleteSelected() = viewModelScope.launch { repository.delete(selected.value); selected.value = emptySet() }
    fun toggleFavorite(id: String) = viewModelScope.launch { repository.toggleFavorite(id) }

    private fun List<Book>.sorted(sort: LibrarySort): List<Book> = when (sort) {
        LibrarySort.RECENT -> sortedWith(compareByDescending<Book> { it.lastReadAt != null }.thenByDescending { it.lastReadAt ?: it.addedAt })
        LibrarySort.ADDED -> sortedByDescending { it.addedAt }
        LibrarySort.TITLE -> sortedBy { it.title.lowercase() }
        LibrarySort.AUTHOR -> sortedBy { it.author.lowercase() }
        LibrarySort.PROGRESS -> sortedByDescending { it.readingProgress }
    }

    private fun List<Book>.filtered(filter: LibraryFilter): List<Book> = when (filter) {
        LibraryFilter.ALL -> this
        LibraryFilter.UNREAD -> filter { it.readingProgress <= .001f }
        LibraryFilter.READING -> filter { it.readingProgress > .001f && it.readingProgress < .995f }
        LibraryFilter.FINISHED -> filter { it.readingProgress >= .995f }
        LibraryFilter.FAVORITE -> filter { it.favorite }
    }
}
