package com.curio.notes.ui.util

import android.content.Context
import android.content.Intent
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.domain.model.Note
import com.curio.notes.ui.components.AiSection
import com.curio.notes.ui.components.sectionsFor

data class ExportedNote(
    val note: Note,
    val aiResponse: AIResponse?,
    val conversation: List<ChatMessage>
)

/** Full markdown export of one note: title, original thought, AI response
 * sections, and conversation turns. Pure function, no Android dependencies. */
fun formatNoteExport(
    note: Note,
    aiResponse: AIResponse?,
    conversation: List<ChatMessage>,
    language: AiLanguage = AiLanguage.ENGLISH
): String = buildString {
    val spanish = language == AiLanguage.SPANISH
    appendLine("# ${note.title.ifBlank { if (spanish) "Sin título" else "Untitled" }}")
    appendLine()
    appendLine(if (spanish) "## Idea original" else "## Original thought")
    appendLine(note.originalText)
    if (aiResponse != null) {
        appendLine()
        appendLine(if (spanish) "## Respuesta de la IA" else "## AI response")
        append(formatAiMarkdown(aiResponse, language))
    }
    if (conversation.isNotEmpty()) {
        appendLine()
        appendLine(if (spanish) "## Conversación" else "## Conversation")
        conversation.forEach { message ->
            val role = when {
                message.role == ChatRole.USER && spanish -> "Tú"
                message.role == ChatRole.USER -> "You"
                spanish -> "IA"
                else -> "AI"
            }
            appendLine("**$role:** ${message.text}")
            appendLine()
        }
    }
}.trim()

/** Joins several note exports with markdown separators. */
fun formatNotesExport(
    items: List<ExportedNote>,
    language: AiLanguage = AiLanguage.ENGLISH
): String = items.joinToString("\n\n---\n\n") { item ->
    formatNoteExport(item.note, item.aiResponse, item.conversation, language)
}

private fun formatAiMarkdown(response: AIResponse, language: AiLanguage): String = buildString {
    sectionsFor(response, language).forEach { section ->
        when (section) {
            is AiSection.Paragraph -> {
                appendLine("### ${section.heading}")
                appendLine(section.body)
                appendLine()
            }
            is AiSection.Bullets -> {
                appendLine("### ${section.heading}")
                section.items.forEach { appendLine("- $it") }
                appendLine()
            }
            is AiSection.Chips -> {
                appendLine("### ${section.heading}")
                appendLine(section.topics.joinToString(", "))
                appendLine()
            }
            is AiSection.Questions -> {
                appendLine("### ${section.heading}")
                section.questions.forEachIndexed { index, question ->
                    appendLine("${index + 1}. $question")
                }
                appendLine()
            }
            is AiSection.Sources -> {
                appendLine("### ${section.heading}")
                section.sources.forEach { appendLine("- [${it.title}](${it.url})") }
                appendLine()
            }
        }
    }
}.trim()

/** Sends text to the system share sheet (ChatGPT, Gemini, or any other app). */
fun shareText(context: Context, text: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
