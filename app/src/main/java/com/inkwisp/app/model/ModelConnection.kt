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
enum class PredictionProtocol {
    Auto,
    ChatContinuation,
    OpenAiFim,
    DeepSeekFim,
    MistralFim,
    OpenAiCompatibleFim,
}

@Serializable
enum class PromptFormat {
    Infer,
    Plain,
    Zeta,
    Zeta2,
    Zeta2_1,
    CodeLlama,
    StarCoder,
    DeepSeekCoder,
    Qwen,
    CodeGemma,
    Codestral,
    Glm,
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
    val predictionProtocol: PredictionProtocol = PredictionProtocol.Auto,
    val predictionBaseUrl: String = "",
    val predictionModelId: String = "",
    val promptFormat: PromptFormat = PromptFormat.Infer,
    val predictionMaxOutputTokens: Int = 180,
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
    val predictionProtocol: PredictionProtocol = PredictionProtocol.Auto,
    val predictionBaseUrl: String = "",
    val predictionModelId: String = "",
    val promptFormat: PromptFormat = PromptFormat.Infer,
    val predictionMaxOutputTokens: Int = 180,
)

enum class PredictionState { Disabled, Idle, Loading, Ready, Error }
