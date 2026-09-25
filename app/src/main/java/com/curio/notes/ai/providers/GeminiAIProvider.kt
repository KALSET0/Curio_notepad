package com.curio.notes.ai.providers

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
import com.curio.notes.ai.prompts.CurioPrompts
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.NoteType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GeminiAIProvider(
    private val apiKey: String,
    private val model: String = GeminiConfig.MODEL,
    private val baseUrl: String = GeminiConfig.BASE_URL,
    private val client: OkHttpClient = defaultClient(),
    private val language: Flow<AppLanguage> = flowOf(AppLanguage.ENGLISH)
) : AIProvider {

    override suspend fun classifyAndAnswer(input: String): AIResponse {
        if (apiKey.isBlank()) throw AiException.MissingApiKey()
        val aiLanguage = language.first().toAiLanguage()
        val body = AiJson.encodeToString(
            GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(CurioPrompts.systemPromptFor(aiLanguage)))
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(CurioPrompts.userPromptFor(input, aiLanguage)))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = aiResponseSchema(),
                    temperature = GeminiConfig.TEMPERATURE,
                    maxOutputTokens = GeminiConfig.MAX_OUTPUT_TOKENS
                )
            )
        )
        val text = postText(body)
        return try {
            AiJson.decodeFromString<AIResponse>(text)
        } catch (e: SerializationException) {
            throw AiException.InvalidResponse(e)
        } catch (e: IllegalArgumentException) {
            throw AiException.InvalidResponse(e)
        }
    }

    override suspend fun continueConversation(
        context: ConversationContext,
        history: List<ChatMessage>,
        input: String
    ): String {
        if (apiKey.isBlank()) throw AiException.MissingApiKey()
        val aiLanguage = language.first().toAiLanguage()
        val contents = buildList {
            add(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(CurioPrompts.conversationContextFor(context, aiLanguage))
                    )
                )
            )
            add(
                GeminiContent(
                    role = "model",
                    parts = listOf(
                        GeminiPart(CurioPrompts.acknowledgementFor(aiLanguage))
                    )
                )
            )
            history.forEach { message ->
                add(
                    GeminiContent(
                        role = if (message.role == ChatRole.USER) "user" else "model",
                        parts = listOf(GeminiPart(message.text))
                    )
                )
            }
            add(GeminiContent(role = "user", parts = listOf(GeminiPart(input))))
        }
        val body = AiJson.encodeToString(
            GeminiChatRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(CurioPrompts.continuationSystemFor(aiLanguage)))
                ),
                contents = contents,
                generationConfig = GeminiChatConfig(
                    temperature = GeminiConfig.TEMPERATURE,
                    maxOutputTokens = GeminiConfig.CHAT_MAX_OUTPUT_TOKENS
                )
            )
        )
        return postText(body)
    }

    private suspend fun postText(body: String): String {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return try {
            execute(request)
        } catch (e: AiException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw AiException.TimedOut(e)
        } catch (e: IOException) {
            throw AiException.Network(e)
        }
    }

    private suspend fun execute(request: Request): String =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCompleted) return
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isCompleted) return
                    response.use { res ->
                        if (!res.isSuccessful) {
                            continuation.resumeWithException(errorFor(res.code))
                            return
                        }
                        val payload = try {
                            AiJson.decodeFromString<GeminiResponse>(res.body.string())
                        } catch (e: Exception) {
                            continuation.resumeWithException(AiException.InvalidResponse(e))
                            return
                        }
                        val text = payload.candidates.firstOrNull()
                            ?.content?.parts?.firstOrNull()?.text
                        if (text.isNullOrBlank()) {
                            continuation.resumeWithException(AiException.InvalidResponse())
                        } else {
                            continuation.resume(text)
                        }
                    }
                }
            })
        }

    private fun errorFor(code: Int): AiException = when (code) {
        400 -> AiException.ServiceError(
            "The request was rejected. Your note is safe — retry, " +
                "and if it keeps failing the prompt may need adjusting."
        )
        401, 403 -> AiException.InvalidApiKey()
        404 -> AiException.ServiceError(
            "The configured model ($model) was not found. " +
                "Your note is safe — check the model name in GeminiConfig."
        )
        429 -> AiException.RateLimited()
        in 500..599 -> AiException.ServiceError(
            "Gemini is having trouble right now (server error $code). " +
                "Your note is saved — retry in a bit."
        )
        else -> AiException.ServiceError(
            "Gemini returned an unexpected status ($code). Your note is safe — retry to try again."
        )
    }

    private fun aiResponseSchema(): JsonObject {
        fun stringField() = buildJsonObject { put("type", "STRING") }
        fun stringListField() = buildJsonObject {
            put("type", "ARRAY")
            put("items", buildJsonObject { put("type", "STRING") })
        }
        return buildJsonObject {
            put("type", "OBJECT")
            put(
                "properties",
                buildJsonObject {
                    put(
                        "type",
                        buildJsonObject {
                            put("type", "STRING")
                            put(
                                "enum",
                                buildJsonArray { NoteType.entries.forEach { add(it.name) } }
                            )
                        }
                    )
                    put("title", stringField())
                    put("summary", stringField())
                    put("explanation", stringField())
                    put("examples", stringListField())
                    put("keyPoints", stringListField())
                    put("relatedTopics", stringListField())
                    put("followUpQuestions", stringListField())
                }
            )
            put(
                "required",
                buildJsonArray {
                    add("type")
                    add("title")
                    add("summary")
                    add("explanation")
                    add("examples")
                    add("keyPoints")
                    add("relatedTopics")
                    add("followUpQuestions")
                }
            )
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(GeminiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(GeminiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(GeminiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(GeminiConfig.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
