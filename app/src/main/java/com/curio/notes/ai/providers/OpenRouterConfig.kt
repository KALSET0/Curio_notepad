package com.curio.notes.ai.providers

object OpenRouterConfig {
    // Free router: OpenRouter picks an available free model that supports
    // the requested features (including structured JSON output).
    // Pin a specific ":free" model here if you prefer a fixed one.
    const val MODEL = "openrouter/free"
    const val BASE_URL = "https://openrouter.ai/api/v1"
    const val TEMPERATURE = 0.4
    const val MAX_OUTPUT_TOKENS = 2048
    const val CHAT_MAX_OUTPUT_TOKENS = 1024
}
