package com.inkwisp.app.ai

import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.model.ModelProtocol
import com.inkwisp.app.model.PredictionProtocol
import com.inkwisp.app.model.PromptFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class ModelGatewayTest {
    private val gateway = ModelGateway()
    private val input = CompletionInput("Continue only", "Before cursor")
    private val predictionInput = PredictionInput("Continue only", "chat context", "before", "after")

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
    fun modelDiscoveryUsesProtocolAuthenticationAndParsesProviderShapes() {
        val openAi = gateway.createModelListRequest(connection(ModelProtocol.OpenAiChat), "secret")
        assertEquals("https://example.test/v1/models", openAi.url.toString())
        assertEquals("Bearer secret", openAi.header("Authorization"))
        assertEquals(
            listOf("gpt-a", "gpt-b"),
            gateway.parseModelListResponse(
                ModelProtocol.OpenAiChat,
                """{"data":[{"id":"gpt-b"},{"id":"gpt-a"}]}""",
            ),
        )

        val anthropic = gateway.createModelListRequest(connection(ModelProtocol.AnthropicMessages), "secret")
        assertEquals("secret", anthropic.header("x-api-key"))
        assertEquals("2023-06-01", anthropic.header("anthropic-version"))

        val gemini = gateway.createModelListRequest(connection(ModelProtocol.GoogleGemini), "secret")
        assertEquals("secret", gemini.url.queryParameter("key"))
        assertEquals("1000", gemini.url.queryParameter("pageSize"))
        assertEquals(
            listOf("gemini-flash"),
            gateway.parseModelListResponse(
                ModelProtocol.GoogleGemini,
                """{"models":[
                  {"baseModelId":"embedding-only","supportedGenerationMethods":["embedContent"]},
                  {"name":"models/gemini-flash","supportedGenerationMethods":["generateContent"]}
                ]}""",
            ),
        )
    }

    @Test
    fun parsesEverySupportedResponseShape() {
        assertEquals("chat", gateway.parseResponse(ModelProtocol.OpenAiChat, """{"choices":[{"message":{"content":"chat"}}]}"""))
        assertEquals("responses", gateway.parseResponse(ModelProtocol.OpenAiResponses, """{"output_text":"responses"}"""))
        assertEquals("anthropic", gateway.parseResponse(ModelProtocol.AnthropicMessages, """{"content":[{"type":"text","text":"anthropic"}]}"""))
        assertEquals("gemini", gateway.parseResponse(ModelProtocol.GoogleGemini, """{"candidates":[{"content":{"parts":[{"text":"gemini"}]}}]}"""))
    }

    @Test
    fun deepSeekV4DisablesThinkingForLowLatencyWriting() {
        val request = gateway.createRequest(
            connection(ModelProtocol.OpenAiChat).copy(modelId = "deepseek-v4-flash"),
            "secret",
            input,
        )
        val buffer = Buffer()
        request.body!!.writeTo(buffer)

        assertTrue(buffer.readUtf8().contains("\"thinking\":{\"type\":\"disabled\"}"))
    }

    @Test
    fun deepSeekFimUsesBetaCompletionWithNativePrefixAndSuffix() {
        val request = gateway.createPredictionRequest(
            connection(ModelProtocol.OpenAiChat).copy(
                baseUrl = "https://api.deepseek.com/v1",
                modelId = "deepseek-v4-flash",
                predictionProtocol = PredictionProtocol.DeepSeekFim,
            ),
            "secret",
            predictionInput,
        )
        val buffer = Buffer()
        request.body!!.writeTo(buffer)
        val body = buffer.readUtf8()

        assertEquals("https://api.deepseek.com/beta/completions", request.url.toString())
        assertTrue(body.contains("\"prompt\":\"before\""))
        assertTrue(body.contains("\"suffix\":\"after\""))
    }

    @Test
    fun mistralFimUsesNativeFimEndpointAndMessageResponse() {
        val request = gateway.createPredictionRequest(
            connection(ModelProtocol.OpenAiChat).copy(
                baseUrl = "https://api.mistral.ai/v1",
                modelId = "codestral-latest",
                predictionProtocol = PredictionProtocol.MistralFim,
            ),
            "secret",
            predictionInput,
        )

        assertEquals("https://api.mistral.ai/v1/fim/completions", request.url.toString())
        assertEquals("inserted", gateway.parseFimResponse("""{"choices":[{"message":{"content":"inserted"}}]}"""))
    }

    @Test
    fun formattedFimEncodesTheSelectedModelPromptAndParsesTextResponse() {
        val request = gateway.createPredictionRequest(
            connection(ModelProtocol.OpenAiChat).copy(
                predictionProtocol = PredictionProtocol.OpenAiCompatibleFim,
                promptFormat = PromptFormat.DeepSeekCoder,
            ),
            "secret",
            predictionInput,
        )
        val buffer = Buffer()
        request.body!!.writeTo(buffer)

        assertTrue(buffer.readUtf8().contains("<｜fim▁begin｜>before<｜fim▁hole｜>after<｜fim▁end｜>"))
        assertEquals("inserted", gateway.parseFimResponse("""{"choices":[{"text":"inserted"}]}"""))
    }

    @Test
    fun successfulProbeRequiresVisibleModelText() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"choices":[{"message":{"content":"OK"}}]}""".toResponseBody())
                .build()
        }.build()
        val probeGateway = ModelGateway(client)

        probeGateway.probe(connection(ModelProtocol.OpenAiChat), "secret", input)
    }

    private fun connection(protocol: ModelProtocol) = ModelConnection(
        name = "Test",
        protocol = protocol,
        baseUrl = "https://example.test/v1",
        modelId = "model",
    )
}
