package com.curio.notes.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class FollowUpList(
    val questions: List<String> = emptyList()
)

// Lenient: malformed output means "keep the old suggestions".
fun parseFollowUpList(json: String?): List<String> =
    json?.let {
        runCatching { AiJson.decodeFromString<FollowUpList>(it).questions }.getOrDefault(emptyList())
    } ?: emptyList()
