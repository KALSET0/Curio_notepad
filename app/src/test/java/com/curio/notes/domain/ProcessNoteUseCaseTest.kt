package com.curio.notes.domain

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
import com.curio.notes.domain.model.NoteStatus
import com.curio.notes.domain.model.NoteType
import com.curio.notes.domain.usecase.ProcessNoteUseCase
import com.curio.notes.testing.FakeNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProcessNoteUseCaseTest {

    @Test
    fun `success stores answer and preserves original`() = runTest {
        val repository = FakeNoteRepository()
        val expected = AIResponse(type = NoteType.CONCEPT, title = "Photo")
        val useCase = ProcessNoteUseCase(repository, FakeAIProvider(response = expected), this)

        val id = repository.createNote("My title", "Define photosynthesis")
        useCase.enqueue(id)
        advanceUntilIdle()

        val saved = repository.observeNote(id).first()!!
        assertEquals(NoteStatus.ANSWERED, saved.status)
        assertEquals(NoteType.CONCEPT, saved.type)
        assertEquals("Define photosynthesis", saved.originalText)
        assertEquals("My title", saved.title)
        assertEquals(expected, AiJson.decodeFromString<AIResponse>(saved.aiResponseJson!!))
    }

    @Test
    fun `failure marks error and preserves original`() = runTest {
        val repository = FakeNoteRepository()
        val failing = FakeAIProvider(failure = IOException("boom"))
        val useCase = ProcessNoteUseCase(repository, failing, this)

        val id = repository.createNote("Title", "Some thought")
        useCase.enqueue(id)
        advanceUntilIdle()

        val saved = repository.observeNote(id).first()!!
        assertEquals(NoteStatus.ERROR, saved.status)
        assertNull(saved.type)
        assertNull(saved.aiResponseJson)
        assertEquals("unknown", saved.errorMessage)
        assertEquals("Some thought", saved.originalText)
        assertEquals("Title", saved.title)
    }

    @Test
    fun `already answered notes are not reprocessed`() = runTest {
        val repository = FakeNoteRepository()
        val provider = FakeAIProvider()
        val useCase = ProcessNoteUseCase(repository, provider, this)

        val id = repository.createNote("Title", "Define photosynthesis")
        repository.updateNote(repository.observeNote(id).first()!!.copy(status = NoteStatus.ANSWERED))
        useCase.enqueue(id)
        advanceUntilIdle()

        assertEquals(0, provider.calls)
    }

    @Test
    fun `missing note does nothing`() = runTest {
        val repository = FakeNoteRepository()
        val provider = FakeAIProvider()
        val useCase = ProcessNoteUseCase(repository, provider, this)

        useCase.enqueue(999L)
        advanceUntilIdle()

        assertEquals(0, provider.calls)
    }

    @Test
    fun `resumePending reprocesses stuck and pending notes`() = runTest {
        val repository = FakeNoteRepository()
        val provider = FakeAIProvider(
            response = AIResponse(type = NoteType.CONCEPT, title = "t")
        )
        val useCase = ProcessNoteUseCase(repository, provider, this)

        val pendingId = repository.createNote("P", "Define things")
        val stuckId = repository.createNote("S", "What is this?")
        val doneId = repository.createNote("D", "Buy milk")
        repository.updateNote(
            repository.observeNote(stuckId).first()!!.copy(status = NoteStatus.PROCESSING)
        )
        repository.updateNote(
            repository.observeNote(doneId).first()!!.copy(status = NoteStatus.ANSWERED)
        )

        useCase.resumePending()
        advanceUntilIdle()

        assertEquals(2, provider.calls)
        assertEquals(NoteStatus.ANSWERED, repository.observeNote(pendingId).first()!!.status)
        assertEquals(NoteStatus.ANSWERED, repository.observeNote(stuckId).first()!!.status)
        assertEquals(NoteStatus.ANSWERED, repository.observeNote(doneId).first()!!.status)
    }

    @Test
    fun `reanalyze resets answer clears conversation and reprocesses`() = runTest {
        val repository = FakeNoteRepository()
        val first = AIResponse(type = NoteType.CONCEPT, title = "First")
        val second = AIResponse(type = NoteType.QUESTION, title = "Second")
        val provider = FakeAIProvider(response = first)
        val useCase = ProcessNoteUseCase(repository, provider, this)

        val id = repository.createNote("Title", "Define photosynthesis")
        useCase.enqueue(id)
        advanceUntilIdle()
        repository.appendConversationMessage(id, ChatMessage(ChatRole.USER, "Hi"))

        provider.response = second
        useCase.reanalyze(id)
        advanceUntilIdle()

        val saved = repository.observeNote(id).first()!!
        assertEquals(NoteStatus.ANSWERED, saved.status)
        assertEquals(NoteType.QUESTION, saved.type)
        assertEquals(second, AiJson.decodeFromString<AIResponse>(saved.aiResponseJson!!))
        assertEquals(2, provider.calls)
        assertTrue(repository.observeConversation(id).first().isEmpty())
    }

    @Test
    fun `reanalyze skips in-flight notes`() = runTest {
        val repository = FakeNoteRepository()
        val provider = FakeAIProvider()
        val useCase = ProcessNoteUseCase(repository, provider, this)

        val id = repository.createNote("Title", "Define photosynthesis")
        repository.updateNote(
            repository.observeNote(id).first()!!.copy(status = NoteStatus.PROCESSING)
        )
        useCase.reanalyze(id)
        advanceUntilIdle()

        assertEquals(0, provider.calls)
    }

    @Test
    fun `reanalyze missing note does nothing`() = runTest {
        val repository = FakeNoteRepository()
        val provider = FakeAIProvider()
        val useCase = ProcessNoteUseCase(repository, provider, this)

        useCase.reanalyze(999L)
        advanceUntilIdle()

        assertEquals(0, provider.calls)
    }

    private class FakeAIProvider(
        var response: AIResponse = AIResponse(type = NoteType.OTHER, title = "t"),
        var failure: IOException? = null,
        var calls: Int = 0
    ) : AIProvider {
        override suspend fun classifyAndAnswer(input: String): AIResponse {
            calls++
            failure?.let { throw it }
            return response
        }

        override suspend fun continueConversation(
            context: ConversationContext,
            history: List<ChatMessage>,
            input: String
        ): String = "fake reply to $input"

        override suspend fun suggestFollowUps(
            context: ConversationContext,
            history: List<ChatMessage>
        ): List<String> = listOf("fake follow-up")
    }

}
