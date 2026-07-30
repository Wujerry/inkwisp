package com.inkwisp.app.ai

import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.model.ModelProtocol
import com.inkwisp.app.model.PredictionProtocol
import com.inkwisp.app.model.PromptFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertThrows

class PredictionRoutingTest {
    @Test
    fun deepSeekAndCodestralUseNativeFimWhileOrdinaryChatStaysChat() {
        assertEquals(PredictionProtocol.DeepSeekFim, resolvePredictionProtocol(connection("https://api.deepseek.com/v1", "deepseek-v4-flash")))
        assertEquals(PredictionProtocol.MistralFim, resolvePredictionProtocol(connection("https://api.mistral.ai/v1", "codestral-latest")))
        assertEquals(PredictionProtocol.ChatContinuation, resolvePredictionProtocol(connection("https://api.openai.com/v1", "gpt-4.1-mini")))
    }

    @Test
    fun infersCommonSelfHostedFimFamiliesWithoutMistakingInstructModels() {
        assertEquals(PromptFormat.DeepSeekCoder, inferPromptFormat("deepseek-ai/deepseek-coder-v2:16b"))
        assertEquals(PromptFormat.Qwen, inferPromptFormat("qwen2.5-coder:7b-base"))
        assertEquals(null, inferPromptFormat("qwen3:8b"))
        assertEquals(null, inferPromptFormat("mistral-small-latest"))
    }

    @Test
    fun explicitPromptFormatActivatesFormattedFimEvenWhenModelNameIsCustom() {
        val custom = connection("http://127.0.0.1:8000/v1", "my-alias")
            .copy(promptFormat = PromptFormat.StarCoder)
        assertEquals(PredictionProtocol.OpenAiCompatibleFim, resolvePredictionProtocol(custom))
    }

    @Test
    fun explicitFormattedFimDoesNotSilentlyGuessAnUnknownModel() {
        val custom = connection("http://127.0.0.1:8000/v1", "my-alias")
            .copy(predictionProtocol = PredictionProtocol.OpenAiCompatibleFim)
        assertThrows(IllegalStateException::class.java) { resolvePromptFormat(custom) }
    }

    @Test
    fun formatsEveryCursorInsertionFimFamily() {
        assertEquals("<PRE> before <SUF>after <MID>", formatFimPrompt(PromptFormat.CodeLlama, "before", "after"))
        assertEquals("<fim_prefix>before<fim_suffix>after<fim_middle>", formatFimPrompt(PromptFormat.StarCoder, "before", "after"))
        assertEquals("<｜fim▁begin｜>before<｜fim▁hole｜>after<｜fim▁end｜>", formatFimPrompt(PromptFormat.DeepSeekCoder, "before", "after"))
        assertEquals("<|fim_prefix|>before<|fim_suffix|>after<|fim_middle|>", formatFimPrompt(PromptFormat.Qwen, "before", "after"))
        assertEquals("[SUFFIX]after[PREFIX]before", formatFimPrompt(PromptFormat.Codestral, "before", "after"))
        assertEquals("<|code_prefix|>before<|code_suffix|>after<|code_middle|>", formatFimPrompt(PromptFormat.Glm, "before", "after"))
    }

    private fun connection(baseUrl: String, modelId: String) = ModelConnection(
        name = "Test",
        protocol = ModelProtocol.OpenAiChat,
        baseUrl = baseUrl,
        modelId = modelId,
    )
}
