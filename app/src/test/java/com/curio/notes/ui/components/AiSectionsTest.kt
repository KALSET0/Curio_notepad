package com.curio.notes.ui.components

import com.curio.notes.ai.AIResponse
import com.curio.notes.domain.model.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSectionsTest {

    @Test
    fun `question leads with quick answer`() {
        val sections = sectionsFor(
            AIResponse(type = NoteType.QUESTION, title = "t", summary = "s", explanation = "e")
        )
        val first = sections.first()
        assertTrue(first is AiSection.Paragraph)
        assertEquals("Quick Answer", (first as AiSection.Paragraph).heading)
    }

    @Test
    fun `empty parts are skipped`() {
        val sections = sectionsFor(AIResponse(type = NoteType.OTHER, title = "t"))
        assertTrue(sections.isEmpty())
    }

    @Test
    fun `idea headings stay neutral and ordered`() {
        val response = AIResponse(
            type = NoteType.IDEA,
            title = "t",
            summary = "s",
            explanation = "e",
            examples = listOf("x"),
            keyPoints = listOf("a", "b"),
            relatedTopics = listOf("r"),
            followUpQuestions = listOf("q")
        )
        val headings = sectionsFor(response).map { section ->
            when (section) {
                is AiSection.Paragraph -> section.heading
                is AiSection.Bullets -> section.heading
                is AiSection.Chips -> section.heading
                is AiSection.Questions -> section.heading
            }
        }
        assertEquals(
            listOf(
                "Interpretation",
                "How It Could Work",
                "Possible Implementation",
                "Points to Weigh",
                "Related Topics",
                "Possible Next Steps"
            ),
            headings
        )
    }

    @Test
    fun `claim separates limits from known facts`() {
        val sections = sectionsFor(
            AIResponse(
                type = NoteType.CLAIM,
                title = "t",
                explanation = "e",
                keyPoints = listOf("k")
            )
        )
        val headings = sections.map { section ->
            when (section) {
                is AiSection.Paragraph -> section.heading
                is AiSection.Bullets -> section.heading
                is AiSection.Chips -> section.heading
                is AiSection.Questions -> section.heading
            }
        }
        assertEquals(listOf("What Is Known", "Context & Limits"), headings)
    }

    @Test
    fun `concept uses definition heading`() {
        val sections = sectionsFor(
            AIResponse(type = NoteType.CONCEPT, title = "t", summary = "s")
        )
        val first = sections.single()
        assertTrue(first is AiSection.Paragraph)
        assertEquals("Definition", (first as AiSection.Paragraph).heading)
    }
}
