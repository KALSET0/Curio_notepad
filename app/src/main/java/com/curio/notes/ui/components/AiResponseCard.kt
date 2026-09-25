package com.curio.notes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.curio.notes.R
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.AiSource
import com.curio.notes.domain.model.NoteType
import com.curio.notes.ui.util.label

sealed interface AiSection {
    data class Paragraph(val heading: String, val body: String) : AiSection
    data class Bullets(val heading: String, val items: List<String>) : AiSection
    data class Chips(val heading: String, val topics: List<String>) : AiSection
    data class Questions(val heading: String, val questions: List<String>) : AiSection
    data class Sources(val heading: String, val sources: List<AiSource>) : AiSection
}

fun sectionsFor(
    response: AIResponse,
    language: AiLanguage = AiLanguage.ENGLISH
): List<AiSection> {
    val headings = headingsFor(response.type, language)
    return buildList {
        response.summary?.takeIf { it.isNotBlank() }?.let {
            add(AiSection.Paragraph(headings.summary, it))
        }
        response.explanation?.takeIf { it.isNotBlank() }?.let {
            add(AiSection.Paragraph(headings.explanation, it))
        }
        response.examples.takeIf { it.isNotEmpty() }?.let {
            add(AiSection.Bullets(headings.examples, it))
        }
        response.keyPoints.takeIf { it.isNotEmpty() }?.let {
            add(AiSection.Bullets(headings.keyPoints, it))
        }
        response.relatedTopics.takeIf { it.isNotEmpty() }?.let {
            add(AiSection.Chips(headings.relatedTopics, it))
        }
        response.followUpQuestions.takeIf { it.isNotEmpty() }?.let {
            add(AiSection.Questions(headings.followUps, it))
        }
        response.sources.takeIf { it.isNotEmpty() }?.let {
            add(AiSection.Sources(sourcesHeading(language), it))
        }
    }
}

private fun sourcesHeading(language: AiLanguage): String =
    if (language == AiLanguage.SPANISH) "Fuentes" else "Sources"

private data class Headings(
    val summary: String,
    val explanation: String,
    val examples: String,
    val keyPoints: String,
    val relatedTopics: String,
    val followUps: String
)

private fun headingsFor(type: NoteType, language: AiLanguage): Headings =
    if (language == AiLanguage.SPANISH) headingsEsFor(type) else headingsEnFor(type)

private fun headingsEnFor(type: NoteType): Headings = when (type) {
    NoteType.QUESTION -> Headings(
        "Quick Answer", "Explanation", "Example",
        "Key Points", "Related Topics", "Follow-up Questions"
    )
    NoteType.CONCEPT -> Headings(
        "Definition", "In Simple Terms", "Example",
        "Key Points", "Related Concepts", "Questions to Explore"
    )
    NoteType.IDEA -> Headings(
        "Interpretation", "How It Could Work", "Possible Implementation",
        "Points to Weigh", "Related Topics", "Possible Next Steps"
    )
    NoteType.CONFUSION -> Headings(
        "What Seems Confusing", "Step by Step", "Example",
        "Key Points", "Related Topics", "Questions to Explore"
    )
    NoteType.TOPIC -> Headings(
        "Overview", "Key Concepts", "Examples",
        "Learning Path", "Related Topics", "Follow-up Questions"
    )
    NoteType.CLAIM -> Headings(
        "The Claim", "What Is Known", "Examples",
        "Context & Limits", "Related Concepts", "Follow-up Questions"
    )
    NoteType.REFLECTION -> Headings(
        "Interpretation", "Different Angles", "Examples",
        "Points to Consider", "Related Topics", "Possible Doubts"
    )
    NoteType.OTHER -> Headings(
        "Summary", "Details", "Examples",
        "Key Points", "Related Topics", "Follow-up Questions"
    )
}

private fun headingsEsFor(type: NoteType): Headings = when (type) {
    NoteType.QUESTION -> Headings(
        "Respuesta rápida", "Explicación", "Ejemplo",
        "Puntos clave", "Temas relacionados", "Preguntas de seguimiento"
    )
    NoteType.CONCEPT -> Headings(
        "Definición", "En palabras simples", "Ejemplo",
        "Puntos clave", "Conceptos relacionados", "Preguntas para explorar"
    )
    NoteType.IDEA -> Headings(
        "Interpretación", "Cómo podría funcionar", "Posible implementación",
        "Aspectos a ponderar", "Temas relacionados", "Posibles próximos pasos"
    )
    NoteType.CONFUSION -> Headings(
        "Lo que parece confuso", "Paso a paso", "Ejemplo",
        "Puntos clave", "Temas relacionados", "Preguntas para explorar"
    )
    NoteType.TOPIC -> Headings(
        "Panorama", "Conceptos clave", "Ejemplos",
        "Ruta de aprendizaje", "Temas relacionados", "Preguntas de seguimiento"
    )
    NoteType.CLAIM -> Headings(
        "La afirmación", "Lo que se sabe", "Ejemplos",
        "Contexto y límites", "Conceptos relacionados", "Preguntas de seguimiento"
    )
    NoteType.REFLECTION -> Headings(
        "Interpretación", "Distintas miradas", "Ejemplos",
        "Puntos a considerar", "Temas relacionados", "Posibles dudas"
    )
    NoteType.OTHER -> Headings(
        "Resumen", "Detalles", "Ejemplos",
        "Puntos clave", "Temas relacionados", "Preguntas de seguimiento"
    )
}

@Composable
fun AiResponseCard(response: AIResponse, modifier: Modifier = Modifier) {
    val language = appAiLanguage()
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ai_header, response.type.label()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                CopyIconButton(
                    text = formatAiResponse(
                        response,
                        includeFollowUpQuestions = false,
                        language = language
                    )
                )
            }
            sectionsFor(response, language).forEach { section ->
                AiSectionContent(section)
            }
        }
    }
}

/** Matches the AI content language to the UI locale. Shared with export. */
@Composable
fun appAiLanguage(): AiLanguage {
    val locale = LocalContext.current.resources.configuration.locales[0]
    return if (locale.language.startsWith("es")) AiLanguage.SPANISH else AiLanguage.ENGLISH
}

@Composable
private fun AiSectionContent(section: AiSection) {
    when (section) {
        is AiSection.Paragraph -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeadingRow(
                    heading = section.heading,
                    copyText = "${section.heading}\n${section.body}"
                )
                Text(text = section.body, style = MaterialTheme.typography.bodyLarge)
            }
        }
        is AiSection.Bullets -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeadingRow(
                    heading = section.heading,
                    copyText = buildString {
                        appendLine(section.heading)
                        section.items.forEach { appendLine("• $it") }
                    }.trim()
                )
                section.items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "•", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        is AiSection.Chips -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeadingRow(
                    heading = section.heading,
                    copyText = "${section.heading}\n${section.topics.joinToString(", ")}"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    section.topics.forEach { topic ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        is AiSection.Questions -> {
            // Display-only for now; tapping a question to continue the
            // conversation arrives with "Continue with AI" (Phase 10).
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeadingRow(
                    heading = section.heading,
                    copyText = buildString {
                        appendLine(section.heading)
                        section.questions.forEachIndexed { index, question ->
                            appendLine("${index + 1}. $question")
                        }
                    }.trim()
                )
                section.questions.forEachIndexed { index, question ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        is AiSection.Sources -> {
            val uriHandler = LocalUriHandler.current
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeadingRow(
                    heading = section.heading,
                    copyText = buildString {
                        appendLine(section.heading)
                        section.sources.forEach { appendLine("- ${it.title} (${it.url})") }
                    }.trim()
                )
                section.sources.forEach { source ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "•", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = source.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { uriHandler.openUri(source.url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun SectionHeadingRow(
    heading: String,
    copyText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeading(heading, modifier = Modifier.weight(1f))
        CopyIconButton(text = copyText)
    }
}

fun formatAiResponse(
    response: AIResponse,
    includeFollowUpQuestions: Boolean = true,
    language: AiLanguage = AiLanguage.ENGLISH
): String = buildString {
    appendLine(response.title)
    sectionsFor(response, language).forEach { section ->
        appendLine()
        when (section) {
            is AiSection.Paragraph -> {
                appendLine(section.heading)
                appendLine(section.body)
            }
            is AiSection.Bullets -> {
                appendLine(section.heading)
                section.items.forEach { appendLine("• $it") }
            }
            is AiSection.Chips -> {
                appendLine(section.heading)
                appendLine(section.topics.joinToString(", "))
            }
            is AiSection.Questions -> {
                if (includeFollowUpQuestions) {
                    appendLine(section.heading)
                    section.questions.forEachIndexed { index, question ->
                        appendLine("${index + 1}. $question")
                    }
                }
            }
            is AiSection.Sources -> {
                appendLine(section.heading)
                section.sources.forEach { appendLine("- ${it.title} (${it.url})") }
            }
        }
    }
}.trim()
