package com.curio.notes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.ai.parseAiResponse
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.repository.NoteRepository
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ui.util.ExportedNote
import com.curio.notes.ui.util.formatNotesExport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<Note>> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = repository.observeArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived

    private val _selectedIds = MutableStateFlow(emptySet<Long>())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _selectionError = MutableStateFlow<String?>(null)
    val selectionError: StateFlow<String?> = _selectionError

    fun setShowArchived(show: Boolean) {
        clearSelection()
        _showArchived.value = show
    }

    fun toggleSelection(noteId: Long) {
        _selectionError.value = null
        _selectedIds.update { if (noteId in it) it - noteId else it + noteId }
    }

    fun enterSelection(noteId: Long) {
        _selectionError.value = null
        _selectedIds.update { it + noteId }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _selectionError.value = null
    }

    fun deleteSelected(onDone: () -> Unit) {
        bulkUpdate(action = { repository.deleteNotes(it) }, onDone = onDone)
    }

    fun archiveSelected() {
        bulkUpdate(action = { repository.setArchived(it, true) })
    }

    fun unarchiveSelected() {
        bulkUpdate(action = { repository.setArchived(it, false) })
    }

    // Single-note swipe action from the inbox list. Errors surface through
    // the same selectionError channel the bulk actions use.
    fun setArchived(noteId: Long, archived: Boolean) {
        viewModelScope.launch {
            try {
                repository.setArchived(setOf(noteId), archived)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _selectionError.value = "update_failed"
            }
        }
    }

    fun pinSelected() {
        bulkUpdate(action = { repository.setPinned(it, true) })
    }

    // Builds the markdown export for the current selection. Conversations
    // load one-shot (no flows) since export is a point-in-time snapshot.
    suspend fun exportSelected(language: AiLanguage): String {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return ""
        val all = notes.value + archivedNotes.value
        val items = ids.mapNotNull { id ->
            all.firstOrNull { it.id == id }?.let { note ->
                ExportedNote(
                    note = note,
                    aiResponse = parseAiResponse(note.aiResponseJson),
                    conversation = repository.observeConversation(id).first()
                )
            }
        }
        return formatNotesExport(items, language)
    }

    private fun bulkUpdate(action: suspend (Set<Long>) -> Unit, onDone: () -> Unit = {}) {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                action(ids)
                _selectedIds.value = emptySet()
                onDone()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _selectionError.value = "update_failed"
            }
        }
    }

    companion object {
        fun factory(repository: NoteRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(repository) as T
            }
    }
}
