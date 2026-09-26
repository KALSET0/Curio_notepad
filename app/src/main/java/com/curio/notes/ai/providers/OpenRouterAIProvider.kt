package com.curio.notes.ai.providers

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.ApiKeyCheck
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

class OpenRouterAIProvider(
    private val apiKey: String,
    private val apiKeyOverride: () -> String = { "" },
    private val model: String = OpenRouterConfig.MODEL,
    private val baseUrl: String = OpenRouterConfig.BASE_URL,
    private val client: OkHttpClient = GeminiAIProvider.defaultClient(),
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
        val body = StrictJson.encodeToString(
            OpenRouterChatRequest(
                model = model,
                messages = listOf(
                    OpenRouterMessage("system", CurioPrompts.systemPromptFor(aiLanguage)),
                    OpenRouterMessage(
                        "user",
                        CurioPrompts.userPromptFor(input, aiLanguage, sources)
                    )
                ),
                temperature = OpenRouterConfig.TEMPERATURE,
                maxTokens = OpenRouterConfig.MAX_OUTPUT_TOKENS,
                responseFormat = OpenRouterResponseFormat("json_object")
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
        val messages = buildList {
            add(OpenRouterMessage("system", CurioPrompts.continuationSystemFor(aiLanguage)))
            add(
                OpenRouterMessage(
                    "user",
                    CurioPrompts.conversationContextFor(context, aiLanguage, sources)
                )
            )
            add(
                OpenRouterMessage(
                    "assistant",
                    CurioPrompts.acknowledgementFor(aiLanguage)
                )
            )
            history.forEach { message ->
                add(
                    OpenRouterMessage(
                        if (message.role == ChatRole.USER) "user" else "assistant",
                        message.text
                    )
                )
            }
            add(OpenRouterMessage("user", input))
        }
        val body = StrictJson.encodeToString(
            OpenRouterChatRequest(
                model = model,
                messages = messages,
                temperature = OpenRouterConfig.TEMPERATURE,
                maxTokens = OpenRouterConfig.CHAT_MAX_OUTPUT_TOKENS
            )
        )
        return postText(body)
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
        val body = StrictJson.encodeToString(
            OpenRouterChatRequest(
                model = model,
                messages = listOf(
                    OpenRouterMessage(
                        "system",
                        CurioPrompts.gatekeeperSystemFor(aiLanguage)
                    ),
                    OpenRouterMessage("user", gatekeeperUserPrompt)
                ),
                temperature = 0.2,
                maxTokens = 128,
                responseFormat = OpenRouterResponseFormat("json_object")
            )
        )
        return parseGatekeeperDecision(postText(body))
    }

    override suspend fun suggestFollowUps(
        context: ConversationContext,
        history: List<ChatMessage>
    ): List<String> {
        if (effectiveKey().isBlank()) throw AiException.MissingApiKey()
        val aiLanguage = language.first().toAiLanguage()
        val body = StrictJson.encodeToString(
            OpenRouterChatRequest(
                model = model,
                messages = listOf(
                    OpenRouterMessage(
                        "system",
                        CurioPrompts.followUpRefreshSystemFor(aiLanguage)
                    ),
                    OpenRouterMessage(
                        "user",
                        CurioPrompts.followUpRefreshPromptFor(
                            context,
                            history,
                            context.followUpQuestions,
                            aiLanguage
                        )
                    )
                ),
                temperature = 0.8,
                maxTokens = 256,
                responseFormat = OpenRouterResponseFormat("json_object")
            )
        )
        return parseFollowUpList(postText(body))
    }

    private suspend fun postText(body: String): String {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .header("Authorization", "Bearer ${effectiveKey()}")
            .header("X-Title", "Curio Notepad")
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
                            AiJson.decodeFromString<OpenRouterChatResponse>(res.body.string())
                        } catch (e: Exception) {
                            continuation.resumeWithException(AiException.InvalidResponse(e))
                            return
                        }
                        val text = payload.choices.firstOrNull()?.message?.content
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
            .header("Authorization", "Bearer $key")
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
                "Your note is safe — check the model name in OpenRouterConfig."
        )
        429 -> AiException.RateLimited()
        in 500..599 -> AiException.ServiceError(
            "The AI service is having trouble right now (server error $code). " +
                "Your note is saved — retry in a bit."
        )
        else -> AiException.ServiceError(
            "The AI service returned an unexpected status ($code). " +
                "Your note is safe — retry to try again."
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val StrictJson = Json(AiJson) {
            explicitNulls = false
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(15L, TimeUnit.SECONDS)
            .callTimeout(90L, TimeUnit.SECONDS)
            .build()
    }
}
