package com.curio.notes.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.ai.GenerationTracker
import com.curio.notes.domain.repository.NoteRepository
import com.curio.notes.domain.usecase.ProcessNoteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreateNoteViewModel(
    private val repository: NoteRepository,
    private val processNoteUseCase: ProcessNoteUseCase,
    tracker: GenerationTracker? = null
) : ViewModel() {
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    val developerMode: StateFlow<Boolean> = (tracker?.developerMode ?: flowOf(false))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val providerLabel: StateFlow<String> = (tracker?.currentLabel ?: flowOf("Mock AI"))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Mock AI")

    fun saveNote(title: String, originalText: String, onSaved: () -> Unit) {
        if (originalText.isBlank()) return
        _saveError.value = null
        viewModelScope.launch {
            try {
                val noteId = repository.createNote(title.trim(), originalText.trim())
                onSaved()
                processNoteUseCase.enqueue(noteId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _saveError.value = "save_failed"
            }
        }
    }

    companion object {
        fun factory(
            repository: NoteRepository,
            processNoteUseCase: ProcessNoteUseCase,
            tracker: GenerationTracker? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CreateNoteViewModel(repository, processNoteUseCase, tracker) as T
            }
    }
}
