package com.curio.notes.ui.search

import com.curio.notes.domain.model.NoteSortOrder
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
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
    fun `blank query yields no results`() = runTest {
        val repository = FakeNoteRepository()
        repository.createNote("Gardening", "Water plants daily")
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.results.value.isEmpty())
    }

    @Test
    fun `query matches title and body`() = runTest {
        val repository = FakeNoteRepository()
        repository.createNote("Gardening tips", "Water plants daily")
        val taxId = repository.createNote("Taxes", "I don't understand taxes")
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect {} }

        viewModel.onQueryChange("tax")
        advanceUntilIdle()

        assertEquals(listOf(taxId), viewModel.results.value.map { it.id })
    }

    @Test
    fun `query matches ai content`() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.createNote("Gardening", "Water plants")
        val note = repository.observeNote(id).first()!!
        repository.updateNote(note.copy(aiResponseJson = """{"summary":"photosynthesis basics"}"""))
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect {} }

        viewModel.onQueryChange("photo")
        advanceUntilIdle()

        assertEquals(listOf(id), viewModel.results.value.map { it.id })
    }

    @Test
    fun `clearing query empties results`() = runTest {
        val repository = FakeNoteRepository()
        repository.createNote("Gardening tips", "Water plants daily")
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect {} }

        viewModel.onQueryChange("garden")
        advanceUntilIdle()
        assertEquals(1, viewModel.results.value.size)

        viewModel.clearQuery()
        advanceUntilIdle()
        assertTrue(viewModel.results.value.isEmpty())
    }

    @Test
    fun `oldest first reverses order`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("First match", "a")
        val second = repository.createNote("Second match", "b")
        repository.updateNote(repository.observeNote(first).first()!!.copy(createdAt = 1_000L))
        repository.updateNote(repository.observeNote(second).first()!!.copy(createdAt = 2_000L))
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect {} }

        viewModel.onQueryChange("match")
        advanceUntilIdle()
        assertEquals(listOf(second, first), viewModel.results.value.map { it.id })

        viewModel.setSortOrder(NoteSortOrder.OLDEST_FIRST)
        advanceUntilIdle()
        assertEquals(listOf(first, second), viewModel.results.value.map { it.id })
    }

    @Test
    fun `type filter narrows results`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("First match", "a")
        repository.createNote("Second match", "b")
        repository.updateNote(
            repository.observeNote(first).first()!!.copy(type = NoteType.QUESTION)
        )
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect {} }

        viewModel.onQueryChange("match")
        advanceUntilIdle()
        assertEquals(2, viewModel.results.value.size)

        viewModel.toggleTypeFilter(NoteType.QUESTION)
        advanceUntilIdle()
        assertEquals(listOf(first), viewModel.results.value.map { it.id })

        viewModel.clearFilters()
        advanceUntilIdle()
        assertEquals(2, viewModel.results.value.size)
    }
}
