package com.curio.notes.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteCardTest {

    @Test
    fun `preview returns the summary`() {
        val json = """{"type":"QUESTION","title":"t","summary":"Short answer."}"""

        assertEquals("Short answer.", aiPreviewFor(json))
    }

    @Test
    fun `preview skips blank and malformed payloads`() {
        assertNull(aiPreviewFor("""{"type":"QUESTION","title":"t","summary":"  "}"""))
        assertNull(aiPreviewFor("""{"type":"QUESTION","title":"t"}"""))
        assertNull(aiPreviewFor("not json"))
        assertNull(aiPreviewFor(null))
    }
}
