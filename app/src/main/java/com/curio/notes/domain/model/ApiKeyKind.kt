package com.curio.notes.domain.model

// Which user credential slot to read/write. Tavily rides the same mechanism
// so public builds can enable web search without build-time secrets.
enum class ApiKeyKind(val prefKey: String) {
    GEMINI("gemini_api_key"),
    OPENROUTER("openrouter_api_key"),
    TAVILY("tavily_api_key")
}

// Pure precedence rule, unit-tested: a user-entered key always wins;
// the build-time key (developer local.properties) is only the fallback.
fun resolveApiKey(userKey: String, buildKey: String): String =
    userKey.ifBlank { buildKey }
