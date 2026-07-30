package com.inkwisp.app.ai

import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.model.ModelProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class CompletionInput(
    val system: String,
    val prompt: String,
)

class ModelGateway(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun complete(
        connection: ModelConnection,
        apiKey: String,
        input: CompletionInput,
    ): String = withContext(Dispatchers.IO) {
        val request = createRequest(connection, apiKey, input)
        val body = execute(request)
        parseResponse(connection.protocol, body).trim()
            .ifEmpty { throw ModelRequestException("The model returned no writing text from ${request.url.host}.") }
    }

    suspend fun probe(
        connection: ModelConnection,
        apiKey: String,
        input: CompletionInput,
    ) = withContext(Dispatchers.IO) {
        execute(createRequest(connection, apiKey, input))
        Unit
    }

    private fun execute(request: Request): String {
        try {
            return client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw ModelRequestException(
                        "HTTP ${response.code} at ${request.url.host}${request.url.encodedPath}: ${extractError(body)}",
                    )
                }
                body
            }
        } catch (failure: IOException) {
            if (failure is ModelRequestException) throw failure
            throw ModelRequestException(
                "Could not reach ${request.url.host}${request.url.encodedPath}: ${failure.message ?: "network error"}",
                failure,
            )
        }
    }

    internal fun createRequest(
        connection: ModelConnection,
        apiKey: String,
        input: CompletionInput,
    ): Request {
        require(connection.baseUrl.startsWith("https://") || connection.baseUrl.startsWith("http://")) {
            "Base URL must start with https:// or http://"
        }
        require(connection.modelId.isNotBlank()) { "Model ID is required." }
        val (url, body) = when (connection.protocol) {
            ModelProtocol.OpenAiChat -> endpoint(connection.baseUrl, "chat/completions") to openAiChatBody(connection, input)
            ModelProtocol.OpenAiResponses -> endpoint(connection.baseUrl, "responses") to openAiResponsesBody(connection, input)
            ModelProtocol.AnthropicMessages -> anthropicEndpoint(connection.baseUrl) to anthropicBody(connection, input)
            ModelProtocol.GoogleGemini -> {
                val model = URLEncoder.encode(connection.modelId, Charsets.UTF_8.name()).replace("+", "%20")
                endpoint(connection.baseUrl, "models/$model:generateContent") to geminiBody(connection, input)
            }
        }
        return Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .apply {
                when (connection.protocol) {
                    ModelProtocol.AnthropicMessages -> {
                        header("x-api-key", apiKey)
                        header("anthropic-version", "2023-06-01")
                    }
                    ModelProtocol.GoogleGemini -> header("x-goog-api-key", apiKey)
                    else -> if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
                }
                connection.headers.forEach { (name, value) -> header(name, value) }
            }
            .build()
    }

    internal fun parseResponse(protocol: ModelProtocol, body: String): String {
        val root = json.parseToJsonElement(body).jsonObject
        return when (protocol) {
            ModelProtocol.OpenAiChat -> root["choices"]?.jsonArray
                ?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            ModelProtocol.OpenAiResponses -> root["output_text"]?.jsonPrimitive?.contentOrNull
                ?: root["output"]?.jsonArray
                    ?.flatMap { it.jsonObject["content"]?.jsonArray.orEmpty() }
                    ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                    ?.joinToString("")
            ModelProtocol.AnthropicMessages -> root["content"]?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                ?.joinToString("")
            ModelProtocol.GoogleGemini -> root["candidates"]?.jsonArray
                ?.firstOrNull()?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                ?.joinToString("")
        }.orEmpty()
    }

    private fun openAiChatBody(connection: ModelConnection, input: CompletionInput): JsonObject = buildJsonObject {
        put("model", JsonPrimitive(connection.modelId))
        put("messages", buildJsonArray {
            add(message("system", input.system))
            add(message("user", input.prompt))
        })
        put("temperature", JsonPrimitive(connection.temperature))
        put("max_tokens", JsonPrimitive(connection.maxOutputTokens))
        if (connection.modelId.startsWith("deepseek-v4", ignoreCase = true)) {
            put("thinking", buildJsonObject { put("type", JsonPrimitive("disabled")) })
        }
        put("stream", JsonPrimitive(false))
    }

    private fun openAiResponsesBody(connection: ModelConnection, input: CompletionInput): JsonObject = buildJsonObject {
        put("model", JsonPrimitive(connection.modelId))
        put("instructions", JsonPrimitive(input.system))
        put("input", JsonPrimitive(input.prompt))
        put("temperature", JsonPrimitive(connection.temperature))
        put("max_output_tokens", JsonPrimitive(connection.maxOutputTokens))
        put("stream", JsonPrimitive(false))
    }

    private fun anthropicBody(connection: ModelConnection, input: CompletionInput): JsonObject = buildJsonObject {
        put("model", JsonPrimitive(connection.modelId))
        put("system", JsonPrimitive(input.system))
        put("messages", buildJsonArray { add(message("user", input.prompt)) })
        put("temperature", JsonPrimitive(connection.temperature))
        put("max_tokens", JsonPrimitive(connection.maxOutputTokens))
        put("stream", JsonPrimitive(false))
    }

    private fun geminiBody(connection: ModelConnection, input: CompletionInput): JsonObject = buildJsonObject {
        put("systemInstruction", buildJsonObject {
            put("parts", buildJsonArray { add(buildJsonObject { put("text", JsonPrimitive(input.system)) }) })
        })
        put("contents", buildJsonArray {
            add(buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("parts", buildJsonArray { add(buildJsonObject { put("text", JsonPrimitive(input.prompt)) }) })
            })
        })
        put("generationConfig", buildJsonObject {
            put("temperature", JsonPrimitive(connection.temperature))
            put("maxOutputTokens", JsonPrimitive(connection.maxOutputTokens))
        })
    }

    private fun message(role: String, content: String): JsonObject = buildJsonObject {
        put("role", JsonPrimitive(role))
        put("content", JsonPrimitive(content))
    }

    private fun endpoint(base: String, path: String): String {
        val normalized = base.trimEnd('/')
        return if (normalized.endsWith("/$path")) normalized else "$normalized/$path"
    }

    private fun anthropicEndpoint(base: String): String {
        val normalized = base.trimEnd('/')
        return when {
            normalized.endsWith("/v1/messages") || normalized.endsWith("/messages") -> normalized
            normalized.endsWith("/v1") -> "$normalized/messages"
            else -> "$normalized/v1/messages"
        }
    }

    private fun extractError(body: String): String = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        root["error"]?.let { error ->
            if (error is JsonPrimitive) error.content
            else error.jsonObject["message"]?.jsonPrimitive?.contentOrNull
        }
    }.getOrNull() ?: body.take(240).ifBlank { "Request failed" }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private class ModelRequestException(message: String, cause: Throwable? = null) : IOException(message, cause)
