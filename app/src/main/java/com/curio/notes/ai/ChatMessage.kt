package com.curio.notes.ai

enum class ChatRole {
    USER,
    MODEL
}

data class ChatMessage(
    val role: ChatRole,
    val text: String
)
