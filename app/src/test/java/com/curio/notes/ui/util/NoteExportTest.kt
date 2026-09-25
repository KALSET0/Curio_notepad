package com.curio.notes.ui.util

import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.AiSource
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.domain.model.Note
import com.curio.notes.domain.model.NoteType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteExportTest {
    private val note = Note(id = 1, title = "Coffee", originalText = "Is coffee true?")

    private val response = AIResponse(
        type = NoteType.QUESTION,
        title = "Coffee",
        summary = "Short answer.",
        keyPoints = listOf("kp"),
        followUpQuestions = listOf("fq"),
        sources = listOf(AiSource("T", "https://example.com/a"))
    )

    private val conversation = listOf(
        ChatMessage(ChatRole.USER, "Tell me more"),
        ChatMessage(ChatRole.MODEL, "More details.")
    )

    @Test
    fun `export contains every section in markdown`() {
        val text = formatNoteExport(note, response, conversation)

        assertTrue(text.contains("# Coffee"))
        assertTrue(text.contains("## Original thought"))
        assertTrue(text.contains("Is coffee true?"))
        assertTrue(text.contains("## AI response"))
        assertTrue(text.contains("### Quick Answer"))
        assertTrue(text.contains("- kp"))
        assertTrue(text.contains("1. fq"))
        assertTrue(text.contains("- [T](https://example.com/a)"))
        assertTrue(text.contains("## Conversation"))
        assertTrue(text.contains("**You:** Tell me more"))
        assertTrue(text.contains("**AI:** More details."))
    }

    @Test
    fun `spanish export is translated`() {
        val text = formatNoteExport(note, response, conversation, AiLanguage.SPANISH)

        assertTrue(text.contains("## Idea original"))
        assertTrue(text.contains("## Respuesta de la IA"))
        assertTrue(text.contains("### Respuesta rápida"))
        assertTrue(text.contains("## Conversación"))
        assertTrue(text.contains("**Tú:** Tell me more"))
        assertTrue(text.contains("**IA:** More details."))
        assertTrue(text.contains("### Fuentes"))
    }

    @Test
    fun `empty parts are skipped`() {
        val text = formatNoteExport(note, null, emptyList())

        assertTrue(text.contains("# Coffee"))
        assertTrue(text.contains("Is coffee true?"))
        assertFalse(text.contains("## AI response"))
        assertFalse(text.contains("## Conversation"))
    }

    @Test
    fun `multiple notes join with separators`() {
        val other = ExportedNote(
            note.copy(id = 2, title = "Tea"),
            aiResponse = null,
            conversation = emptyList()
        )
        val text = formatNotesExport(
            listOf(ExportedNote(note, response, conversation), other)
        )

        assertTrue(text.contains("# Coffee"))
        assertTrue(text.contains("# Tea"))
        assertTrue(text.contains("---"))
    }
}
