package com.curio.notes.ai

import java.io.IOException

sealed class AiException(val code: String, message: String, cause: Throwable? = null) :
    IOException(message, cause) {
    class MissingApiKey : AiException(
        "missing_api_key",
        "No Gemini API key is configured. " +
            "Add GEMINI_API_KEY to local.properties (or the environment) and rebuild."
    )

    class InvalidApiKey : AiException(
        "invalid_api_key",
        "The Gemini API key was rejected. Check GEMINI_API_KEY and try again."
    )

    class Network(cause: Throwable) : AiException(
        "network",
        "No connection to the AI service. Your note is saved — retry when you're back online.",
        cause
    )

    class TimedOut(cause: Throwable? = null) : AiException(
        "timed_out",
        "The AI took too long to respond. Your note is saved — try again.",
        cause
    )

    class RateLimited : AiException(
        "rate_limited",
        "Gemini rate limit reached. Wait a minute and retry."
    )

    class InvalidResponse(cause: Throwable? = null) : AiException(
        "invalid_response",
        "The AI returned something unexpected. Your note is safe — retry to try again.",
        cause
    )

    class ServiceError(details: String) : AiException("service_error", details)

    // Web search (Tavily) specifically: key missing/rejected. Surfaced instead
    // of answering silently without sources the user explicitly enabled.
    class SearchFailed : AiException(
        "search_failed",
        "Web search is not available. " +
            "Add TAVILY_API_KEY to local.properties (or the environment) and rebuild."
    )

    // Local AI (Ollama): server URL missing. The laptop server must be
    // configured in Developer options before this provider can run.
    class OllamaNotConfigured : AiException(
        "ollama_not_configured",
        "The local AI server is not configured. " +
            "Set its URL in Settings → Developer options."
    )
}
