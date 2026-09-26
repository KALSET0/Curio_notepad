package com.curio.notes.ui.home

import com.curio.notes.testing.FakeNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
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
    fun `long press enters selection mode`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        repository.createNote("B", "b")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch { viewModel.selectedIds.collect {} }
        advanceUntilIdle()

        viewModel.enterSelection(first)
        advanceUntilIdle()

        assertEquals(setOf(first), viewModel.selectedIds.value)
    }

    @Test
    fun `tap toggles selection`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        val second = repository.createNote("B", "b")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch { viewModel.selectedIds.collect {} }
        advanceUntilIdle()

        viewModel.toggleSelection(first)
        viewModel.toggleSelection(second)
        viewModel.toggleSelection(first)
        advanceUntilIdle()

        assertEquals(setOf(second), viewModel.selectedIds.value)

        viewModel.clearSelection()
        advanceUntilIdle()

        assertTrue(viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun `deleteSelected removes notes and clears`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        val second = repository.createNote("B", "b")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch {
            viewModel.notes.collect {}
            viewModel.selectedIds.collect {}
        }
        advanceUntilIdle()

        viewModel.enterSelection(first)
        viewModel.toggleSelection(second)
        var done = false
        viewModel.deleteSelected { done = true }
        advanceUntilIdle()

        assertTrue(done)
        assertTrue(viewModel.selectedIds.value.isEmpty())
        assertTrue(viewModel.notes.value.isEmpty())
    }

    @Test
    fun `deleteSelected with empty selection does nothing`() = runTest {
        val repository = FakeNoteRepository()
        repository.createNote("A", "a")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch { viewModel.selectedIds.collect {} }
        backgroundScope.launch { viewModel.notes.collect {} }
        advanceUntilIdle()

        var done = false
        viewModel.deleteSelected { done = true }
        advanceUntilIdle()

        assertFalse(done)
        assertEquals(1, viewModel.notes.value.size)
    }

    @Test
    fun `archiveSelected archives and clears`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        repository.createNote("B", "b")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch { viewModel.notes.collect {} }
        backgroundScope.launch { viewModel.archivedNotes.collect {} }
        backgroundScope.launch { viewModel.selectedIds.collect {} }
        advanceUntilIdle()

        viewModel.enterSelection(first)
        viewModel.archiveSelected()
        advanceUntilIdle()

        assertTrue(viewModel.selectedIds.value.isEmpty())
        assertEquals(1, viewModel.notes.value.size)
        assertEquals(listOf(first), viewModel.archivedNotes.value.map { it.id })
    }

    @Test
    fun `unarchiveSelected restores to inbox`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        repository.createNote("B", "b")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch { viewModel.notes.collect {} }
        backgroundScope.launch { viewModel.archivedNotes.collect {} }
        backgroundScope.launch { viewModel.selectedIds.collect {} }
        advanceUntilIdle()

        viewModel.enterSelection(first)
        viewModel.archiveSelected()
        advanceUntilIdle()
        viewModel.enterSelection(first)
        viewModel.unarchiveSelected()
        advanceUntilIdle()

        assertEquals(2, viewModel.notes.value.size)
        assertTrue(viewModel.archivedNotes.value.isEmpty())
    }

    @Test
    fun `setArchived moves single note both ways`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        repository.createNote("B", "b")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch { viewModel.notes.collect {} }
        backgroundScope.launch { viewModel.archivedNotes.collect {} }
        advanceUntilIdle()

        viewModel.setArchived(first, true)
        advanceUntilIdle()

        assertEquals(1, viewModel.notes.value.size)
        assertEquals(listOf(first), viewModel.archivedNotes.value.map { it.id })

        viewModel.setArchived(first, false)
        advanceUntilIdle()

        assertEquals(2, viewModel.notes.value.size)
        assertTrue(viewModel.archivedNotes.value.isEmpty())
    }

    @Test
    fun `pinSelected pins note to top`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        val second = repository.createNote("B", "b")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch {
            viewModel.notes.collect {}
            viewModel.selectedIds.collect {}
        }
        advanceUntilIdle()

        viewModel.enterSelection(first)
        viewModel.pinSelected()
        advanceUntilIdle()

        assertEquals(listOf(first, second), viewModel.notes.value.map { it.id })
        assertTrue(viewModel.notes.value.first().isPinned)
    }

    @Test
    fun `switching tabs clears selection`() = runTest {
        val repository = FakeNoteRepository()
        val first = repository.createNote("A", "a")
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch {
            viewModel.selectedIds.collect {}
            viewModel.showArchived.collect {}
        }
        advanceUntilIdle()

        viewModel.enterSelection(first)
        viewModel.setShowArchived(true)
        advanceUntilIdle()

        assertTrue(viewModel.selectedIds.value.isEmpty())
        assertTrue(viewModel.showArchived.value)
    }
}
