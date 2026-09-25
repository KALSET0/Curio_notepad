package com.curio.notes.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteSortOrder
import com.curio.notes.domain.model.NoteType
import com.curio.notes.domain.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: NoteRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sortOrder = MutableStateFlow(NoteSortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder

    private val _selectedTypes = MutableStateFlow(emptySet<NoteType>())
    val selectedTypes: StateFlow<Set<NoteType>> = _selectedTypes

    val results: StateFlow<List<Note>> = combine(
        _query.debounce(300),
        _sortOrder,
        _selectedTypes
    ) { q, sort, types -> Triple(q, sort, types) }
        .flatMapLatest { (q, sort, types) ->
            if (q.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchNotes(q.trim()).map { list ->
                    list
                        .filter { note -> types.isEmpty() || note.type in types }
                        .let { filtered ->
                            if (sort == NoteSortOrder.OLDEST_FIRST) {
                                filtered.sortedBy { it.createdAt }
                            } else {
                                filtered.sortedByDescending { it.createdAt }
                            }
                        }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.update { value }
    }

    fun clearQuery() {
        _query.update { "" }
    }

    fun setSortOrder(order: NoteSortOrder) {
        _sortOrder.value = order
    }

    fun toggleTypeFilter(type: NoteType) {
        _selectedTypes.update { if (type in it) it - type else it + type }
    }

    fun clearFilters() {
        _selectedTypes.value = emptySet()
        _sortOrder.value = NoteSortOrder.NEWEST_FIRST
    }

    companion object {
        fun factory(repository: NoteRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SearchViewModel(repository) as T
            }
    }
}
