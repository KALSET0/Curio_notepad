package com.curio.notes.data.repository

internal fun deriveTitle(text: String): String {
    val firstLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
    if (firstLine.isEmpty()) return "Untitled"
    return if (firstLine.length <= 50) firstLine else firstLine.take(50).trimEnd() + "…"
}
