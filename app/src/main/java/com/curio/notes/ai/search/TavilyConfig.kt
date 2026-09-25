package com.curio.notes.ai.search

object TavilyConfig {
    const val BASE_URL = "https://api.tavily.com"
    const val MAX_RESULTS = 5
    // Short enough to keep prompts lean; snippets are context, not content.
    const val MAX_SNIPPET_CHARS = 300
}
