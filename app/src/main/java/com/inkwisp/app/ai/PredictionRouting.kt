package com.inkwisp.app.ai

import com.inkwisp.app.model.ModelConnection
import com.inkwisp.app.model.PredictionProtocol
import com.inkwisp.app.model.PromptFormat

internal fun resolvePredictionProtocol(connection: ModelConnection): PredictionProtocol {
    if (connection.predictionProtocol != PredictionProtocol.Auto) return connection.predictionProtocol
    val host = runCatching { java.net.URI(connection.baseUrl).host.orEmpty().lowercase() }.getOrDefault("")
    val model = predictionModel(connection).lowercase()
    val format = if (connection.promptFormat == PromptFormat.Infer) inferPromptFormat(model)
    else connection.promptFormat
    return when {
        host == "api.deepseek.com" -> PredictionProtocol.DeepSeekFim
        host.endsWith("mistral.ai") && model.contains("codestral") -> PredictionProtocol.MistralFim
        format?.supportsCursorInsertion == true && format != PromptFormat.Plain -> PredictionProtocol.OpenAiCompatibleFim
        else -> PredictionProtocol.ChatContinuation
    }
}

internal fun predictionModel(connection: ModelConnection): String =
    connection.predictionModelId.ifBlank { connection.modelId }

internal fun resolvePromptFormat(connection: ModelConnection): PromptFormat =
    if (connection.promptFormat == PromptFormat.Infer) {
        inferPromptFormat(predictionModel(connection))
            ?: error("InkWisp could not infer a FIM prompt format from model '${predictionModel(connection)}'. Choose one explicitly.")
    } else connection.promptFormat

internal fun inferPromptFormat(modelId: String): PromptFormat? {
    val model = modelId.substringAfterLast('/').substringBefore(':').lowercase()
    return when {
        model == "zeta" || model == "zeta1" -> PromptFormat.Zeta
        model == "zeta2" -> PromptFormat.Zeta2
        model == "zeta2.1" -> PromptFormat.Zeta2_1
        model == "codellama" || model == "code-llama" || model.startsWith("codellama-") -> PromptFormat.CodeLlama
        model.startsWith("starcoder") -> PromptFormat.StarCoder
        model.startsWith("deepseek-coder") -> PromptFormat.DeepSeekCoder
        model == "qwen" || model.startsWith("qwen-coder") || model.startsWith("qwen2.5-coder") -> PromptFormat.Qwen
        model.startsWith("codegemma") -> PromptFormat.CodeGemma
        model == "codestral" || model.startsWith("codestral-") || model == "mistral" -> PromptFormat.Codestral
        model == "glm" || model.startsWith("glm-4") -> PromptFormat.Glm
        else -> null
    }
}

internal val PromptFormat.supportsCursorInsertion: Boolean
    get() = this !in setOf(PromptFormat.Zeta, PromptFormat.Zeta2, PromptFormat.Zeta2_1)

internal fun formatFimPrompt(format: PromptFormat, prefix: String, suffix: String): String = when (format) {
    PromptFormat.Infer -> error("Prompt format must be resolved before formatting.")
    PromptFormat.Plain -> prefix
    PromptFormat.CodeLlama -> "<PRE> $prefix <SUF>$suffix <MID>"
    PromptFormat.StarCoder -> "<fim_prefix>$prefix<fim_suffix>$suffix<fim_middle>"
    PromptFormat.DeepSeekCoder -> "<｜fim▁begin｜>$prefix<｜fim▁hole｜>$suffix<｜fim▁end｜>"
    PromptFormat.Qwen, PromptFormat.CodeGemma ->
        "<|fim_prefix|>$prefix<|fim_suffix|>$suffix<|fim_middle|>"
    PromptFormat.Codestral -> "[SUFFIX]$suffix[PREFIX]$prefix"
    PromptFormat.Glm -> "<|code_prefix|>$prefix<|code_suffix|>$suffix<|code_middle|>"
    PromptFormat.Zeta, PromptFormat.Zeta2, PromptFormat.Zeta2_1 -> error(
        "Zeta predicts rewritten edit regions and cannot be used as cursor-only FIM.",
    )
}

internal val STANDARD_FIM_STOP_TOKENS = listOf(
    "<|endoftext|>", "<|file_separator|>", "<|fim_pad|>",
    "<|fim_prefix|>", "<|fim_middle|>", "<|fim_suffix|>",
    "<fim_prefix>", "<fim_middle>", "<fim_suffix>",
    "<PRE>", "<SUF>", "<MID>", "[PREFIX]", "[SUFFIX]",
)
