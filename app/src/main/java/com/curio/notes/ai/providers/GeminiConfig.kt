package com.curio.notes.ai.providers

object GeminiConfig {
    // The exact model lives here and nowhere else.
    const val MODEL = "gemini-3.8-flash"
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    const val TEMPERATURE = 0.4
    const val MAX_OUTPUT_TOKENS = 2048
    const val CHAT_MAX_OUTPUT_TOKENS = 1024
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 60L
    const val CALL_TIMEOUT_SECONDS = 90L
}
