package com.curio.notes.ai.providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class GeminiRequest(
    @SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig
)

@Serializable
internal data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
internal data class GeminiPart(val text: String)

@Serializable
internal data class GeminiGenerationConfig(
    @SerialName("responseMimeType") val responseMimeType: String,
    @SerialName("responseSchema") val responseSchema: JsonObject,
    val temperature: Double,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@Serializable
internal data class GeminiChatRequest(
    @SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiChatConfig
)

@Serializable
internal data class GeminiChatConfig(
    val temperature: Double,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int
)
