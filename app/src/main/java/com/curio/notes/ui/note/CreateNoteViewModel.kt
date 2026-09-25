package com.curio.notes.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.domain.repository.NoteRepository
import com.curio.notes.domain.usecase.ProcessNoteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreateNoteViewModel(
    private val repository: NoteRepository,
    private val processNoteUseCase: ProcessNoteUseCase
) : ViewModel() {
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

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
            processNoteUseCase: ProcessNoteUseCase
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CreateNoteViewModel(repository, processNoteUseCase) as T
            }
    }
}
