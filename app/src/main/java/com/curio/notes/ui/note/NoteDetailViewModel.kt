package com.curio.notes.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.parseAiResponse
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.repository.NoteRepository
import com.curio.notes.domain.usecase.ProcessNoteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteDetailViewModel(
    private val repository: NoteRepository,
    private val processNoteUseCase: ProcessNoteUseCase,
    private val noteId: Long
) : ViewModel() {
    val note: StateFlow<Note?> = repository.observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Malformed payloads decode to null so the UI can fall back gracefully.
    val aiResponse: StateFlow<AIResponse?> = note
        .map { current -> parseAiResponse(current?.aiResponseJson) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError

    fun saveChanges(title: String, originalText: String) {
        val current = note.value ?: return
        if (originalText.isBlank()) return
        _operationError.value = null
        viewModelScope.launch {
            try {
                repository.updateNote(
                    current.copy(
                        title = title.ifBlank { current.title },
                        originalText = originalText
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _operationError.value = "save_changes_failed"
            }
        }
    }

    fun retryProcessing() {
        val current = note.value ?: return
        if (current.status != NoteStatus.ERROR && current.status != NoteStatus.PENDING) return
        processNoteUseCase.enqueue(current.id)
    }

    fun deleteNote(onDeleted: () -> Unit) {
        val current = note.value ?: return
        _operationError.value = null
        viewModelScope.launch {
            try {
                repository.deleteNote(current.id)
                onDeleted()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _operationError.value = "delete_failed"
            }
        }
    }

    fun toggleArchive() {
        val current = note.value ?: return
        updateFlag { repository.setArchived(setOf(current.id), !current.isArchived) }
    }

    fun togglePin() {
        val current = note.value ?: return
        updateFlag { repository.setPinned(setOf(current.id), !current.isPinned) }
    }

    private fun updateFlag(action: suspend () -> Unit) {
        _operationError.value = null
        viewModelScope.launch {
            try {
                action()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _operationError.value = "update_failed"
            }
        }
    }

    companion object {
        fun factory(
            repository: NoteRepository,
            processNoteUseCase: ProcessNoteUseCase,
            noteId: Long
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NoteDetailViewModel(repository, processNoteUseCase, noteId) as T
            }
    }
}
