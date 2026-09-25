package com.curio.notes.domain.usecase

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.repository.NoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

class ProcessNoteUseCase(
    private val repository: NoteRepository,
    private val aiProvider: AIProvider,
    private val scope: CoroutineScope
) {
    fun enqueue(noteId: Long) {
        scope.launch { process(noteId) }
    }

    suspend fun resumePending() {
        repository.resetStuckProcessingNotes()
        repository.getPendingNoteIds().forEach { enqueue(it) }
    }

    suspend fun process(noteId: Long) {
        val note = repository.observeNote(noteId).firstOrNull() ?: return
        if (note.status != NoteStatus.PENDING && note.status != NoteStatus.ERROR) return
        repository.updateNote(note.copy(status = NoteStatus.PROCESSING, errorMessage = null))
        try {
            val response = aiProvider.classifyAndAnswer(note.originalText)
            repository.updateNote(
                note.copy(
                    type = response.type,
                    aiResponseJson = AiJson.encodeToString(response),
                    status = NoteStatus.ANSWERED,
                    errorMessage = null
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val code = (e as? AiException)?.code ?: "unknown"
            repository.updateNote(note.copy(status = NoteStatus.ERROR, errorMessage = code))
        }
    }
}
