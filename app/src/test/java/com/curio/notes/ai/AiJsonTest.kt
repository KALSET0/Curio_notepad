package com.curio.notes.ai

import com.curio.notes.domain.model.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiJsonTest {

    @Test
    fun `parses valid payload`() {
        val parsed = parseAiResponse("""{"type":"QUESTION","title":"Q"}""")
        assertEquals(NoteType.QUESTION, parsed?.type)
        assertEquals("Q", parsed?.title)
    }

    @Test
    fun `returns null for garbage and null`() {
        assertNull(parseAiResponse("not json"))
        assertNull(parseAiResponse(null))
    }
}
