package com.curio.notes.ai.search

import com.curio.notes.ai.AiException
import com.curio.notes.ai.AiJson
import com.curio.notes.ai.AiLanguage
import com.curio.notes.ai.providers.GeminiAIProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TavilyWebSearch(
    private val apiKey: String,
    private val baseUrl: String = TavilyConfig.BASE_URL,
    private val client: OkHttpClient = GeminiAIProvider.defaultClient()
) : WebSearchProvider {

    override suspend fun search(
        query: String,
        maxResults: Int,
        language: AiLanguage
    ): List<WebResult> {
        if (apiKey.isBlank()) throw AiException.MissingApiKey()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val body = AiJson.encodeToString(
            TavilySearchRequest(
                apiKey = apiKey,
                query = trimmed,
                maxResults = maxResults.coerceIn(1, 10)
            )
        )
        val payload = try {
            AiJson.decodeFromString<TavilySearchResponse>(postText(body))
        } catch (e: SerializationException) {
            throw AiException.InvalidResponse(e)
        } catch (e: IllegalArgumentException) {
            throw AiException.InvalidResponse(e)
        }
        return payload.results
            .filter { it.url.isNotBlank() }
            .take(maxResults.coerceIn(1, 10))
            .map {
                WebResult(
                    title = it.title.ifBlank { it.url },
                    url = it.url,
                    snippet = it.content.take(TavilyConfig.MAX_SNIPPET_CHARS)
                )
            }
    }

    private suspend fun postText(body: String): String {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/search")
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
        401, 403 -> AiException.InvalidApiKey()
        429 -> AiException.RateLimited()
        else -> AiException.ServiceError(
            "Tavily returned an unexpected status ($code). " +
                "Your note is safe — answering without fresh sources."
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
