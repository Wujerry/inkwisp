package com.inkwisp.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ModelProtocol {
    OpenAiChat,
    OpenAiResponses,
    AnthropicMessages,
    GoogleGemini,
}

@Serializable
data class ModelConnection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: ModelProtocol,
    val baseUrl: String,
    val modelId: String,
    val headers: Map<String, String> = emptyMap(),
    val temperature: Double = 0.3,
    val maxOutputTokens: Int = 180,
    val enabled: Boolean = true,
    val requiresApiKey: Boolean = true,
)

data class ConnectionDraft(
    val id: String? = null,
    val name: String = "",
    val protocol: ModelProtocol = ModelProtocol.OpenAiChat,
    val baseUrl: String = "",
    val modelId: String = "",
    val apiKey: String = "",
    val requiresApiKey: Boolean = true,
    val dataTransferAccepted: Boolean = false,
)

enum class PredictionState { Disabled, Idle, Loading, Ready, Error }
