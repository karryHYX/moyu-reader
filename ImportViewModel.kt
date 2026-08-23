package com.moyu.reader.ui.importbook

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moyu.reader.data.BookImportRepository
import com.moyu.reader.data.ImportProgress
import com.moyu.reader.data.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportUiState(
    val running: Boolean = false,
    val current: ImportProgress? = null,
    val completed: List<ImportResult> = emptyList(),
    val discoveredCount: Int = 0,
)

class ImportViewModel(private val importer: BookImportRepository) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()
    private var lastUris: List<Uri> = emptyList()

    fun importUris(uris: List<Uri>, charsetOverride: String? = null) {
        if (uris.isEmpty() || _state.value.running) return
        lastUris = uris
        viewModelScope.launch {
            _state.value = ImportUiState(running = true, discoveredCount = uris.size)
            val results = ArrayList<ImportResult>()
            for (uri in uris) {
                val result = importer.import(uri, charsetOverride) { progress ->
                    _state.value = _state.value.copy(current = progress, completed = results.toList())
                }
                results += result
                _state.value = _state.value.copy(completed = results.toList())
            }
            _state.value = _state.value.copy(running = false, current = null, completed = results)
        }
    }

    fun scanAndImport(treeUri: Uri) {
        if (_state.value.running) return
        viewModelScope.launch {
            _state.value = ImportUiState(running = true)
            val files = importer.scanTree(treeUri)
            _state.value = _state.value.copy(running = false, discoveredCount = files.size)
            importUris(files)
        }
    }

    fun clearResults() { if (!_state.value.running) _state.value = ImportUiState() }
    fun retryWithCharset(charset: String) {
        if (lastUris.isNotEmpty() && !_state.value.running) {
            _state.value = ImportUiState()
            importUris(lastUris, charset)
        }
    }
}
