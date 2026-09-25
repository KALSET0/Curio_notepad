package com.curio.notes.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
import com.curio.notes.ai.GenerationTracker
import com.curio.notes.ai.parseAiResponse
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.repository.NoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val repository: NoteRepository,
    private val aiProvider: AIProvider,
    private val noteId: Long,
    // Optional telemetry for developer mode. Null in unit tests.
    private val tracker: GenerationTracker? = null
) : ViewModel() {
    val note: StateFlow<Note?> = repository.observeNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val aiResponse: StateFlow<AIResponse?> = note
        .map { current -> parseAiResponse(current?.aiResponseJson) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Conversation history is persisted per note: reopening the chat
    // restores everything the user and the AI already said.
    val messages: StateFlow<List<ChatMessage>> = repository.observeConversation(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val isReady: StateFlow<Boolean> = note
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val developerMode: StateFlow<Boolean> = (tracker?.developerMode ?: flowOf(false))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun send(input: String) {
        val text = input.trim()
        if (text.isBlank() || _isSending.value) return
        if (buildContext() == null) return
        // Snapshot history before appending: the provider expects
        // everything said so far, excluding the new user message.
        fetchReply(history = messages.value, input = text, appendUser = true)
    }

    fun retry() {
        val current = messages.value
        val lastUser = current.lastOrNull()
        if (lastUser?.role != ChatRole.USER || _isSending.value) return
        if (buildContext() == null) return
        fetchReply(history = current.dropLast(1), input = lastUser.text, appendUser = false)
    }

    fun dismissError() {
        _error.value = null
    }

    private fun fetchReply(history: List<ChatMessage>, input: String, appendUser: Boolean) {
        val context = buildContext() ?: return
        _isSending.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                if (appendUser) {
                    repository.appendConversationMessage(noteId, ChatMessage(ChatRole.USER, input))
                }
                val reply = if (tracker != null) {
                    tracker.track(noteId) {
                        aiProvider.continueConversation(context, history, input)
                    }
                } else {
                    aiProvider.continueConversation(context, history, input)
                }
                val stats = tracker?.statsFor(noteId)
                repository.appendConversationMessage(
                    noteId,
                    ChatMessage(
                        role = ChatRole.MODEL,
                        text = reply,
                        generationLabel = stats?.label,
                        generationMillis = stats?.durationMillis
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = (e as? AiException)?.code ?: "unknown"
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun buildContext(): ConversationContext? {
        val current = note.value ?: return null
        val response = aiResponse.value
        return ConversationContext(
            originalText = current.originalText,
            type = current.type,
            summary = response?.summary,
            relatedTopics = response?.relatedTopics.orEmpty()
        )
    }

    companion object {
        fun factory(
            repository: NoteRepository,
            aiProvider: AIProvider,
            noteId: Long,
            tracker: GenerationTracker? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ConversationViewModel(repository, aiProvider, noteId, tracker) as T
            }
    }
}
