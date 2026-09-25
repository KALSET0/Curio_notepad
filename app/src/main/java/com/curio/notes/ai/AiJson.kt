package com.curio.notes.ai

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val AiJson = Json {
    ignoreUnknownKeys = true
}

fun parseAiResponse(json: String?): AIResponse? =
    json?.let { runCatching { AiJson.decodeFromString<AIResponse>(it) }.getOrNull() }
