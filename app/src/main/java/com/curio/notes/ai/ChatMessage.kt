package com.curio.notes.ai

enum class ChatRole {
    USER,
    MODEL
}

data class ChatMessage(
    val role: ChatRole,
    val text: String,
    // Developer-mode diagnostics, set only on MODEL messages. Only .text
    // is ever sent to providers; defaults keep existing call sites compiling.
    val generationLabel: String? = null,
    val generationMillis: Long? = null
)
