package com.curio.notes.ai.providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("response_format") val responseFormat: OpenRouterResponseFormat? = null
)

@Serializable
internal data class OpenRouterMessage(val role: String, val content: String)

@Serializable
internal data class OpenRouterResponseFormat(val type: String)

@Serializable
internal data class OpenRouterChatResponse(val choices: List<OpenRouterChoice> = emptyList())

@Serializable
internal data class OpenRouterChoice(val message: OpenRouterMessage? = null)
