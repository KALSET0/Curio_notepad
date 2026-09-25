package com.curio.notes.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTitlesTest {

    @Test
    fun `blank derives untitled`() {
        assertEquals("Untitled", deriveTitle("   \n  "))
    }

    @Test
    fun `short text stays intact`() {
        assertEquals("Hello world", deriveTitle("Hello world"))
    }

    @Test
    fun `first line wins`() {
        assertEquals("First", deriveTitle("First\nSecond"))
    }

    @Test
    fun `long titles truncate with ellipsis`() {
        val title = deriveTitle("a".repeat(100))
        assertEquals(51, title.length)
        assertTrue(title.endsWith("…"))
    }
}
