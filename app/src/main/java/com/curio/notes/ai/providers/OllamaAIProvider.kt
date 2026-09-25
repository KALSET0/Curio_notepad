package com.curio.notes.ai.providers

import com.curio.notes.ai.AIProvider
import com.curio.notes.ai.AIResponse
import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.ChatMessage
import com.curio.notes.ai.ChatRole
import com.curio.notes.ai.ConversationContext
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

const val OLLAMA_DEFAULT_MODEL = "llama3.1"

// Reasoning traces some local models leak (qwen3 <think> blocks) are
// stripped from user-facing text. Untagged text passes through untouched.
// (?s) lets . span lines, (?i) matches <THINK> too.
internal fun stripThinkBlocks(text: String): String =
    text.replace(Regex("(?si)<think>.*?</think>"), "").trim()

// User-typed server address normalized to the OpenAI-compatible base URL:
// scheme defaulted to http, trailing slashes trimmed, /v1 ensured.
// Returns "" when the input cannot be a server address.
internal fun normalizeOllamaBaseUrl(raw: String): String {
    var text = raw.trim()
    if (text.isEmpty()) return ""
    if (!text.contains("://")) text = "http://$text"
    if (!text.startsWith("http://") && !text.startsWith("https://")) return ""
    val withoutScheme = text.substringAfter("://")
    if (withoutScheme.isBlank() || withoutScheme.startsWith("/")) return ""
    text = text.trimEnd('/')
    if (!text.endsWith("/v1")) text += "/v1"
    return text
}

class OllamaAIProvider(
    private val serverUrl: Flow<String> = flowOf(""),
    private val modelName: Flow<String> = flowOf(OLLAMA_DEFAULT_MODEL),
    private val client: OkHttpClient = GeminiAIProvider.defaultClient(),
    private val language: Flow<AppLanguage> = flowOf(AppLanguage.ENGLISH),
    private val webSearch: WebSearchProvider? = null,
    private val webSearchEnabled: Flow<Boolean> = flowOf(false)
) : AIProvider {

    override suspend fun classifyAndAnswer(input: String): AIResponse {
        val (url, model) = endpoint()
        val aiLanguage = language.first().toAiLanguage()
        val sources = researchWithPrompt(
            CurioPrompts.gatekeeperPromptFor(input, aiLanguage),
            aiLanguage,
            url,
            model
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
                temperature = OllamaConfig.TEMPERATURE,
                maxTokens = OllamaConfig.MAX_OUTPUT_TOKENS,
                responseFormat = OpenRouterResponseFormat("json_object")
            )
        )
        val text = postText(url, body)
        return try {
            AiJson.decodeFromString<AIResponse>(
                stripThinkBlocks(extractContent(text))
            ).withVerifiedSources(sources)
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
        val (url, model) = endpoint()
        val aiLanguage = language.first().toAiLanguage()
        // The gatekeeper decides once, on the first turn. Later turns reuse
        // the grounded answer already present in history.
        val sources = if (history.isEmpty()) {
            researchWithPrompt(
                CurioPrompts.gatekeeperPromptForConversation(context, input, aiLanguage),
                aiLanguage,
                url,
                model
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
                temperature = OllamaConfig.TEMPERATURE,
                maxTokens = OllamaConfig.CHAT_MAX_OUTPUT_TOKENS
            )
        )
        val reply = stripThinkBlocks(extractContent(postText(url, body)))
        if (reply.isBlank()) throw AiException.InvalidResponse()
        return reply
    }

    // Quick reachability check for the Developer options test button.
    // Never throws: true only on a 2xx answer from /api/tags. Uses a
    // short real call timeout instead of withTimeoutOrNull, which cannot
    // work here (callers may run on virtual time).
    suspend fun testConnection(): Boolean {
        val raw = try {
            serverUrl.first()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return false
        }
        val root = normalizeOllamaBaseUrl(raw).removeSuffix("/v1")
        if (root.isEmpty()) return false
        return try {
            execute(
                Request.Builder().url("$root/api/tags").get().build(),
                testClient
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun endpoint(): Pair<String, String> {
        val url = normalizeOllamaBaseUrl(serverUrl.first())
        if (url.isEmpty()) throw AiException.OllamaNotConfigured()
        val model = modelName.first().trim().ifBlank { OLLAMA_DEFAULT_MODEL }
        return url to model
    }

    // Runs the gatekeeper mini-call and, when approved, the web search.
    // Gatekeeper trouble degrades to no sources; key problems surface.
    private suspend fun researchWithPrompt(
        gatekeeperUserPrompt: String,
        aiLanguage: AiLanguage,
        url: String,
        model: String
    ): List<WebResult> {
        val engine = webSearch ?: return emptyList()
        if (!webSearchEnabled.first()) return emptyList()
        val decision = try {
            askGatekeeper(gatekeeperUserPrompt, aiLanguage, url, model)
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
        aiLanguage: AiLanguage,
        url: String,
        model: String
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
        return parseGatekeeperDecision(stripThinkBlocks(extractContent(postText(url, body))))
    }

    private fun extractContent(envelope: String): String {
        val content = try {
            AiJson.decodeFromString<OpenRouterChatResponse>(envelope)
                .choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            throw AiException.InvalidResponse(e)
        }
        if (content.isNullOrBlank()) throw AiException.InvalidResponse()
        return content
    }

    private suspend fun postText(url: String, body: String): String {
        val request = Request.Builder()
            .url("${url.trimEnd('/')}/chat/completions")
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
        execute(request, client)

    private suspend fun execute(request: Request, callClient: OkHttpClient): String =
        suspendCancellableCoroutine { continuation ->
            val call = callClient.newCall(request)
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
                        val text = res.body.string()
                        if (text.isBlank()) {
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
                "and if it keeps failing the model may not support JSON mode."
        )
        401, 403 -> AiException.InvalidApiKey()
        404 -> AiException.ServiceError(
            "The local server did not answer at this address. " +
                "Your note is safe — check the URL in Developer options."
        )
        429 -> AiException.RateLimited()
        in 500..599 -> AiException.ServiceError(
            "The local model is having trouble right now (server error $code). " +
                "Your note is saved — retry in a bit."
        )
        else -> AiException.ServiceError(
            "The local server returned an unexpected status ($code). " +
                "Your note is safe — retry to try again."
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val StrictJson = Json(AiJson) {
            explicitNulls = false
        }

        private val testClient: OkHttpClient by lazy {
            GeminiAIProvider.defaultClient().newBuilder()
                .connectTimeout(5L, TimeUnit.SECONDS)
                .readTimeout(5L, TimeUnit.SECONDS)
                .writeTimeout(5L, TimeUnit.SECONDS)
                .callTimeout(OllamaConfig.TEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build()
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(15L, TimeUnit.SECONDS)
            .callTimeout(90L, TimeUnit.SECONDS)
            .build()
    }
}
