package com.inkwisp.app.ai

import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.model.ModelProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelGatewayTest {
    private val gateway = ModelGateway()
    private val input = CompletionInput("Continue only", "Before cursor")

    @Test
    fun openAiChatUsesBearerHeaderAndChatEndpoint() {
        val request = gateway.createRequest(connection(ModelProtocol.OpenAiChat), "secret", input)
        assertEquals("https://example.test/v1/chat/completions", request.url.toString())
        assertEquals("Bearer secret", request.header("Authorization"))
        assertTrue(request.body.toString().isNotBlank())
    }

    @Test
    fun anthropicUsesProviderHeaders() {
        val request = gateway.createRequest(connection(ModelProtocol.AnthropicMessages), "secret", input)
        assertEquals("secret", request.header("x-api-key"))
        assertEquals("2023-06-01", request.header("anthropic-version"))
    }

    @Test
    fun anthropicCompatibleBaseUrlUsesSdkCompatibleV1MessagesPath() {
        val request = gateway.createRequest(
            connection(ModelProtocol.AnthropicMessages).copy(
                baseUrl = "https://api.deepseek.com/anthropic",
                modelId = "deepseek-v4-flash",
            ),
            "secret",
            input,
        )

        assertEquals("https://api.deepseek.com/anthropic/v1/messages", request.url.toString())
    }

    @Test
    fun keylessOpenAiCompatibleConnectionOmitsAuthorization() {
        val request = gateway.createRequest(
            connection(ModelProtocol.OpenAiChat).copy(requiresApiKey = false),
            "",
            input,
        )
        assertEquals(null, request.header("Authorization"))
    }

    @Test
    fun parsesEverySupportedResponseShape() {
        assertEquals("chat", gateway.parseResponse(ModelProtocol.OpenAiChat, """{"choices":[{"message":{"content":"chat"}}]}"""))
        assertEquals("responses", gateway.parseResponse(ModelProtocol.OpenAiResponses, """{"output_text":"responses"}"""))
        assertEquals("anthropic", gateway.parseResponse(ModelProtocol.AnthropicMessages, """{"content":[{"type":"text","text":"anthropic"}]}"""))
        assertEquals("gemini", gateway.parseResponse(ModelProtocol.GoogleGemini, """{"candidates":[{"content":{"parts":[{"text":"gemini"}]}}]}"""))
    }

    private fun connection(protocol: ModelProtocol) = ModelConnection(
        name = "Test",
        protocol = protocol,
        baseUrl = "https://example.test/v1",
        modelId = "model",
    )
}
