package com.curio.notes.ai.providers

object OllamaConfig {
    // Placeholder default shown in Developer options; the real address is
    // typed by the user (their laptop on the same Wi-Fi).
    const val BASE_URL = "http://192.168.1.10:11434/v1"
    const val TEMPERATURE = 0.4
    const val MAX_OUTPUT_TOKENS = 2048
    const val CHAT_MAX_OUTPUT_TOKENS = 1024
    const val TEST_TIMEOUT_MILLIS = 8_000L
}
