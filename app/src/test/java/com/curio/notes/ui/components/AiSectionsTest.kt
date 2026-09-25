package com.curio.notes.ui.components

import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.AiSource
import com.curio.notes.ai.withVerifiedSources
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
                is AiSection.Sources -> section.heading
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
                is AiSection.Sources -> section.heading
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

    @Test
    fun `formatted response contains title and all parts`() {
        val response = AIResponse(
            type = NoteType.QUESTION,
            title = "Quantum?",
            summary = "Short answer.",
            examples = listOf("ex"),
            keyPoints = listOf("kp"),
            relatedTopics = listOf("rt"),
            followUpQuestions = listOf("fq")
        )
        val formatted = formatAiResponse(response)

        assertTrue(formatted.contains("Quantum?"))
        assertTrue(formatted.contains("Quick Answer"))
        assertTrue(formatted.contains("• ex"))
        assertTrue(formatted.contains("1. fq"))
        assertTrue(formatted.contains("rt"))
    }

    @Test
    fun `formatted response can exclude follow-up questions`() {
        val response = AIResponse(
            type = NoteType.QUESTION,
            title = "Quantum?",
            summary = "Short answer.",
            followUpQuestions = listOf("fq")
        )
        val formatted = formatAiResponse(response, includeFollowUpQuestions = false)

        assertTrue(formatted.contains("Quantum?"))
        assertTrue(formatted.contains("Short answer."))
        assertTrue(!formatted.contains("fq"))
        assertTrue(!formatted.contains("Follow-up Questions"))
    }

    @Test
    fun `spanish headings are translated`() {
        val sections = sectionsFor(
            AIResponse(type = NoteType.QUESTION, title = "t", summary = "s"),
            AiLanguage.SPANISH
        )
        val first = sections.single()
        assertTrue(first is AiSection.Paragraph)
        assertEquals("Respuesta rápida", (first as AiSection.Paragraph).heading)
    }

    @Test
    fun `spanish formatted response uses spanish headings`() {
        val response = AIResponse(
            type = NoteType.CONCEPT,
            title = "Fotosíntesis",
            summary = "Definición.",
            followUpQuestions = listOf("fq")
        )
        val formatted = formatAiResponse(response, language = AiLanguage.SPANISH)

        assertTrue(formatted.contains("Definición"))
        assertTrue(!formatted.contains("Definition"))
    }

    @Test
    fun `reflection follow-ups read as possible doubts`() {
        val response = AIResponse(
            type = NoteType.REFLECTION,
            title = "t",
            followUpQuestions = listOf("q")
        )
        val english = sectionsFor(response).single()
        assertTrue(english is AiSection.Questions)
        assertEquals("Possible Doubts", (english as AiSection.Questions).heading)

        val spanish = sectionsFor(response, AiLanguage.SPANISH).single()
        assertTrue(spanish is AiSection.Questions)
        assertEquals("Posibles dudas", (spanish as AiSection.Questions).heading)
    }

    @Test
    fun `sources section renders in both languages`() {
        val response = AIResponse(
            type = NoteType.QUESTION,
            title = "t",
            sources = listOf(AiSource("T", "https://example.com/a"))
        )
        val english = sectionsFor(response).single()
        assertTrue(english is AiSection.Sources)
        assertEquals("Sources", (english as AiSection.Sources).heading)

        val spanish = sectionsFor(response, AiLanguage.SPANISH).single()
        assertTrue(spanish is AiSection.Sources)
        assertEquals("Fuentes", (spanish as AiSection.Sources).heading)
    }

    @Test
    fun `formatted response includes sources even without follow-ups`() {
        val response = AIResponse(
            type = NoteType.QUESTION,
            title = "t",
            followUpQuestions = listOf("fq"),
            sources = listOf(AiSource("T", "https://example.com/a"))
        )
        val formatted = formatAiResponse(response, includeFollowUpQuestions = false)

        assertTrue(!formatted.contains("fq"))
        assertTrue(formatted.contains("Sources"))
        assertTrue(formatted.contains("https://example.com/a"))
    }

    @Test
    fun `unverified source urls are dropped`() {
        val response = AIResponse(
            type = NoteType.QUESTION,
            title = "t",
            sources = listOf(
                AiSource("Real", "https://example.com/a"),
                AiSource("Invented", "https://hallucinated.example/x")
            )
        )
        val verified = response.withVerifiedSources(
            listOf(
                com.curio.notes.ai.search.WebResult("T", "https://example.com/a", "s")
            )
        )

        assertEquals(1, verified.sources.size)
        assertEquals("https://example.com/a", verified.sources.single().url)
    }

    @Test
    fun `old payloads without sources still parse`() {
        val parsed = com.curio.notes.ai.parseAiResponse(
            """{"type":"QUESTION","title":"t"}"""
        )

        assertEquals(NoteType.QUESTION, parsed?.type)
        assertTrue(parsed?.sources.isNullOrEmpty())
    }
}
