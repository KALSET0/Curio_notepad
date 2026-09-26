package com.curio.notes.ai.providers

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.ApiKeyCheck
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
import com.curio.notes.ai.parseFollowUpList
import com.curio.notes.ai.prompts.CurioPrompts
import com.curio.notes.ai.search.GatekeeperDecision
import com.curio.notes.ai.search.TavilyConfig
import com.curio.notes.ai.search.WebResult
import com.curio.notes.ai.search.WebSearchProvider
import com.curio.notes.ai.search.executeSearch
import com.curio.notes.ai.search.parseGatekeeperDecision
import com.curio.notes.ai.withVerifiedSources
import com.curio.notes.domain.model.AppLanguage
import com.curio.notes.domain.model.NoteType
import kotlinx.coroutines.CancellationException
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
    private val apiKeyOverride: () -> String = { "" },
    private val model: String = GeminiConfig.MODEL,
    private val baseUrl: String = GeminiConfig.BASE_URL,
    private val client: OkHttpClient = defaultClient(),
    private val language: Flow<AppLanguage> = flowOf(AppLanguage.ENGLISH),
    private val webSearch: WebSearchProvider? = null,
    private val webSearchEnabled: Flow<Boolean> = flowOf(false)
) : AIProvider {

    // User-entered key (Settings/Setup) wins; the build-time key is only the
    // developer fallback. Read per call so a key change applies instantly.
    private fun effectiveKey(): String = apiKeyOverride().ifBlank { apiKey }

    override suspend fun classifyAndAnswer(input: String): AIResponse {
        if (effectiveKey().isBlank()) throw AiException.MissingApiKey()
        val aiLanguage = language.first().toAiLanguage()
        val sources = researchWithPrompt(
            CurioPrompts.gatekeeperPromptFor(input, aiLanguage),
            aiLanguage
        )
        val body = AiJson.encodeToString(
            GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(CurioPrompts.systemPromptFor(aiLanguage)))
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(CurioPrompts.userPromptFor(input, aiLanguage, sources))
                        )
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
            AiJson.decodeFromString<AIResponse>(text).withVerifiedSources(sources)
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
        if (effectiveKey().isBlank()) throw AiException.MissingApiKey()
        val aiLanguage = language.first().toAiLanguage()
        // The gatekeeper decides once, on the first turn. Later turns reuse
        // the grounded answer already present in history.
        val sources = if (history.isEmpty()) {
            researchWithPrompt(
                CurioPrompts.gatekeeperPromptForConversation(context, input, aiLanguage),
                aiLanguage
            )
        } else {
            emptyList()
        }
        val contents = buildList {
            add(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(
                            CurioPrompts.conversationContextFor(context, aiLanguage, sources)
                        )
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

    override suspend fun suggestFollowUps(
        context: ConversationContext,
        history: List<ChatMessage>
    ): List<String> {
        if (effectiveKey().isBlank()) throw AiException.MissingApiKey()
        val aiLanguage = language.first().toAiLanguage()
        val body = AiJson.encodeToString(
            GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(CurioPrompts.followUpRefreshSystemFor(aiLanguage))
                    )
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(
                                CurioPrompts.followUpRefreshPromptFor(
                                    context,
                                    history,
                                    context.followUpQuestions,
                                    aiLanguage
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = followUpSchema(),
                    temperature = 0.8,
                    maxOutputTokens = 256
                )
            )
        )
        return parseFollowUpList(postText(body))
    }

    private fun followUpSchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        put(
            "properties",
            buildJsonObject {
                put(
                    "questions",
                    buildJsonObject {
                        put("type", "ARRAY")
                        put("items", buildJsonObject { put("type", "STRING") })
                    }
                )
            }
        )
        put(
            "required",
            buildJsonArray { add("questions") }
        )
    }

    private suspend fun postText(body: String): String {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/models/$model:generateContent")
            .header("x-goog-api-key", effectiveKey())
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

    // Runs the gatekeeper mini-call and, when approved, the web search.
    // Gatekeeper trouble degrades to no sources; key problems surface.
    private suspend fun researchWithPrompt(
        gatekeeperUserPrompt: String,
        aiLanguage: AiLanguage
    ): List<WebResult> {
        val engine = webSearch ?: return emptyList()
        if (!webSearchEnabled.first()) return emptyList()
        val decision = try {
            askGatekeeper(gatekeeperUserPrompt, aiLanguage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return emptyList()
        }
        if (!decision.need_search) return emptyList()
        return engine.executeSearch(decision.query, TavilyConfig.MAX_RESULTS, aiLanguage)
    }

    private suspend fun askGatekeeper(
        gatekeeperUserPrompt: String,
        aiLanguage: AiLanguage
    ): GatekeeperDecision {
        val body = AiJson.encodeToString(
            GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(CurioPrompts.gatekeeperSystemFor(aiLanguage))
                    )
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(gatekeeperUserPrompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = gatekeeperSchema(),
                    temperature = 0.2,
                    maxOutputTokens = 128
                )
            )
        )
        return parseGatekeeperDecision(postText(body))
    }

    private fun gatekeeperSchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        put(
            "properties",
            buildJsonObject {
                put("need_search", buildJsonObject { put("type", "BOOLEAN") })
                put("query", buildJsonObject { put("type", "STRING") })
            }
        )
        put(
            "required",
            buildJsonArray {
                add("need_search")
                add("query")
            }
        )
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

    // Lightweight key check for Settings/Setup: lists models and maps only
    // what the UI distinguishes (accepted / rejected / unreachable).
    suspend fun validateKey(key: String): ApiKeyCheck {
        if (key.isBlank()) return ApiKeyCheck.INVALID
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/models")
            .header("x-goog-api-key", key)
            .get()
            .build()
        val code = try {
            statusOf(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ApiKeyCheck.UNREACHABLE
        }
        return when (code) {
            200 -> ApiKeyCheck.VALID
            400, 401, 403 -> ApiKeyCheck.INVALID
            else -> ApiKeyCheck.UNREACHABLE
        }
    }

    private suspend fun statusOf(request: Request): Int =
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
                    response.use { res -> continuation.resume(res.code) }
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
                    put(
                        "sources",
                        buildJsonObject {
                            put("type", "ARRAY")
                            put(
                                "items",
                                buildJsonObject {
                                    put("type", "OBJECT")
                                    put(
                                        "properties",
                                        buildJsonObject {
                                            put("title", stringField())
                                            put("url", stringField())
                                        }
                                    )
                                    put(
                                        "required",
                                        buildJsonArray {
                                            add("title")
                                            add("url")
                                        }
                                    )
                                }
                            )
                        }
                    )
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
                    add("sources")
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
