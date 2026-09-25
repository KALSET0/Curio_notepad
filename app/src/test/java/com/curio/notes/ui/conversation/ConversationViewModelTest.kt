package com.curio.notes.ui.conversation

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
import com.curio.notes.domain.model.NoteType
import com.curio.notes.testing.FakeNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `send appends user and model messages`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val fake = FakeConversationAI()
        val viewModel = ConversationViewModel(repository, fake, id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        advanceUntilIdle()

        viewModel.send("Tell me more")
        advanceUntilIdle()

        assertEquals(
            listOf(
                ChatMessage(ChatRole.USER, "Tell me more"),
                ChatMessage(ChatRole.MODEL, "Reply to: Tell me more")
            ),
            viewModel.messages.value
        )
        assertEquals(false, viewModel.isSending.value)
        assertNull(viewModel.error.value)
        assertTrue(fake.lastHistory.isEmpty())
    }

    @Test
    fun `second send includes earlier messages as history`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val fake = FakeConversationAI()
        val viewModel = ConversationViewModel(repository, fake, id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        advanceUntilIdle()

        viewModel.send("First")
        advanceUntilIdle()
        viewModel.send("Second")
        advanceUntilIdle()

        assertEquals(4, viewModel.messages.value.size)
        assertEquals(2, fake.lastHistory.size)
    }

    @Test
    fun `failure keeps user message and retry recovers`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val fake = FakeConversationAI(failure = IOException("boom"))
        val viewModel = ConversationViewModel(repository, fake, id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        advanceUntilIdle()

        viewModel.send("Tell me more")
        advanceUntilIdle()

        assertEquals(1, viewModel.messages.value.size)
        assertTrue(viewModel.error.value?.isNotBlank() == true)

        fake.failure = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, viewModel.messages.value.size)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `blank input is ignored`() = runTest {        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val fake = FakeConversationAI()
        val viewModel = ConversationViewModel(repository, fake, id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        advanceUntilIdle()

        viewModel.send("   ")
        advanceUntilIdle()

        assertTrue(viewModel.messages.value.isEmpty())
        assertEquals(0, fake.calls)
    }

    @Test
    fun `exposes decoded ai response for suggestions`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val stored = repository.observeNote(id).first()!!
        repository.updateNote(
            stored.copy(
                aiResponseJson = AiJson.encodeToString(
                    AIResponse(type = NoteType.QUESTION, title = "Q")
                )
            )
        )
        val viewModel = ConversationViewModel(repository, FakeConversationAI(), id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.aiResponse.collect {} }
        advanceUntilIdle()

        assertEquals(NoteType.QUESTION, viewModel.aiResponse.value?.type)
    }

    @Test
    fun `messages persist across view model instances`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val first = ConversationViewModel(repository, FakeConversationAI(), id)
        backgroundScope.launch { first.note.collect {} }
        backgroundScope.launch { first.messages.collect {} }
        advanceUntilIdle()

        first.send("Tell me more")
        advanceUntilIdle()
        assertEquals(2, first.messages.value.size)

        val second = ConversationViewModel(repository, FakeConversationAI(), id)
        backgroundScope.launch { second.note.collect {} }
        backgroundScope.launch { second.messages.collect {} }
        advanceUntilIdle()

        assertEquals(
            listOf(
                ChatMessage(ChatRole.USER, "Tell me more"),
                ChatMessage(ChatRole.MODEL, "Reply to: Tell me more")
            ),
            second.messages.value
        )
    }

    @Test
    fun `resend drops reply and refetches with edited text`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val fake = FakeConversationAI()
        val viewModel = ConversationViewModel(repository, fake, id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        advanceUntilIdle()

        viewModel.send("First")
        advanceUntilIdle()
        assertEquals(2, viewModel.messages.value.size)

        viewModel.resend(0, "Edited")
        advanceUntilIdle()

        assertEquals(
            listOf(
                ChatMessage(ChatRole.USER, "Edited"),
                ChatMessage(ChatRole.MODEL, "Reply to: Edited")
            ),
            viewModel.messages.value
        )
        assertTrue(fake.lastHistory.isEmpty())
        assertEquals(2, fake.calls)
    }

    @Test
    fun `resend with same text regenerates the reply`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val fake = FakeConversationAI()
        val viewModel = ConversationViewModel(repository, fake, id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        advanceUntilIdle()

        viewModel.send("First")
        advanceUntilIdle()
        viewModel.resend(0, "First")
        advanceUntilIdle()

        assertEquals(2, viewModel.messages.value.size)
        assertEquals(
            listOf(
                ChatMessage(ChatRole.USER, "First"),
                ChatMessage(ChatRole.MODEL, "Reply to: First")
            ),
            viewModel.messages.value
        )
        assertEquals(2, fake.calls)
    }

    @Test
    fun `resend ignores invalid targets`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Title", "Define photosynthesis")
        val fake = FakeConversationAI()
        val viewModel = ConversationViewModel(repository, fake, id)
        backgroundScope.launch { viewModel.note.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        advanceUntilIdle()

        viewModel.send("First")
        advanceUntilIdle()

        viewModel.resend(1, "Nope")
        viewModel.resend(5, "Nope")
        viewModel.resend(-1, "Nope")
        viewModel.resend(0, "   ")
        advanceUntilIdle()

        assertEquals(2, viewModel.messages.value.size)
        assertEquals(1, fake.calls)
    }

    private class FakeConversationAI(
        var failure: IOException? = null,
        var calls: Int = 0,
        var lastHistory: List<ChatMessage> = emptyList()
    ) : AIProvider {
        override suspend fun classifyAndAnswer(input: String): AIResponse =
            AIResponse(type = NoteType.OTHER, title = "t")

        override suspend fun continueConversation(
            context: ConversationContext,
            history: List<ChatMessage>,
            input: String
        ): String {
            calls++
            lastHistory = history
            failure?.let { throw it }
            return "Reply to: $input"
        }
    }
}
