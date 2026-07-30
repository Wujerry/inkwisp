package com.inkwisp.app.model

data class ProviderPreset(
    val id: String,
    val name: String,
    val category: ProviderCategory,
    val protocol: ModelProtocol,
    val baseUrl: String,
    val exampleModel: String,
    val requiresApiKey: Boolean = true,
)

enum class ProviderCategory { Official, Aggregator, China, Local, Custom }

/**
 * Provider presets are conveniences, not vendor-specific integrations. Every preset still
 * uses one of InkWisp's protocol adapters and all populated values remain editable.
 */
object ProviderCatalog {
    val presets: List<ProviderPreset> = listOf(
        preset("openai", "OpenAI", ProviderCategory.Official, ModelProtocol.OpenAiResponses, "https://api.openai.com/v1", "gpt-4.1-mini"),
        preset("anthropic", "Anthropic", ProviderCategory.Official, ModelProtocol.AnthropicMessages, "https://api.anthropic.com/v1", "claude-sonnet-4-5"),
        preset("google", "Google Gemini", ProviderCategory.Official, ModelProtocol.GoogleGemini, "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash"),
        preset("deepseek", "DeepSeek", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://api.deepseek.com/v1", "deepseek-v4-flash"),
        preset("deepseek-anthropic", "DeepSeek · Anthropic API", ProviderCategory.Official, ModelProtocol.AnthropicMessages, "https://api.deepseek.com/anthropic", "deepseek-v4-flash"),
        preset("xai", "xAI", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://api.x.ai/v1", "grok-4"),
        preset("mistral", "Mistral AI", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://api.mistral.ai/v1", "mistral-small-latest"),
        preset("cohere", "Cohere", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://api.cohere.ai/compatibility/v1", "command-r-plus"),
        preset("perplexity", "Perplexity", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://api.perplexity.ai", "sonar"),
        preset("groq", "Groq", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://api.groq.com/openai/v1", "openai/gpt-oss-120b"),
        preset("cerebras", "Cerebras", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://api.cerebras.ai/v1", "gpt-oss-120b"),
        preset("nvidia", "NVIDIA NIM", ProviderCategory.Official, ModelProtocol.OpenAiChat, "https://integrate.api.nvidia.com/v1", "meta/llama-3.3-70b-instruct"),
        preset("together", "Together AI", ProviderCategory.Aggregator, ModelProtocol.OpenAiChat, "https://api.together.xyz/v1", "meta-llama/Llama-3.3-70B-Instruct-Turbo"),
        preset("fireworks", "Fireworks AI", ProviderCategory.Aggregator, ModelProtocol.OpenAiChat, "https://api.fireworks.ai/inference/v1", "accounts/fireworks/models/llama-v3p3-70b-instruct"),
        preset("openrouter", "OpenRouter", ProviderCategory.Aggregator, ModelProtocol.OpenAiChat, "https://openrouter.ai/api/v1", "openrouter/auto"),
        preset("huggingface", "Hugging Face Inference", ProviderCategory.Aggregator, ModelProtocol.OpenAiChat, "https://router.huggingface.co/v1", "openai/gpt-oss-120b"),
        preset("vercel", "Vercel AI Gateway", ProviderCategory.Aggregator, ModelProtocol.OpenAiChat, "https://ai-gateway.vercel.sh/v1", "openai/gpt-4.1-mini"),
        preset("github-models", "GitHub Models", ProviderCategory.Aggregator, ModelProtocol.OpenAiChat, "https://models.github.ai/inference", "openai/gpt-4.1-mini"),
        preset("dashscope", "Alibaba Cloud Model Studio", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
        preset("siliconflow", "SiliconFlow", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.siliconflow.cn/v1", "deepseek-ai/DeepSeek-V3"),
        preset("moonshot", "Moonshot AI", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.moonshot.cn/v1", "moonshot-v1-32k"),
        preset("kimi-code", "Kimi Coding", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.kimi.com/coding/v1", "kimi-for-coding"),
        preset("zhipu", "Zhipu AI / BigModel", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://open.bigmodel.cn/api/paas/v4", "glm-4.5-flash"),
        preset("minimax-cn", "MiniMax China", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.minimaxi.com/v1", "MiniMax-M2.5"),
        preset("minimax-global", "MiniMax Global", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.minimax.io/v1", "MiniMax-M2.5"),
        preset("volcengine", "Volcengine Ark", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://ark.cn-beijing.volces.com/api/v3", "doubao-seed-1-6-flash-250828"),
        preset("baidu", "Baidu Qianfan", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://qianfan.baidubce.com/v2", "ernie-4.5-turbo-128k"),
        preset("tencent", "Tencent Hunyuan", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.hunyuan.cloud.tencent.com/v1", "hunyuan-turbos-latest"),
        preset("stepfun", "StepFun", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.stepfun.com/v1", "step-2-16k"),
        preset("yi", "01.AI / Yi", ProviderCategory.China, ModelProtocol.OpenAiChat, "https://api.lingyiwanwu.com/v1", "yi-lightning"),
        preset("xiaomi", "Xiaomi MiMo", ProviderCategory.China, ModelProtocol.AnthropicMessages, "https://api.xiaomimimo.com/anthropic", "mimo-v2-pro"),
        preset("ollama", "Ollama", ProviderCategory.Local, ModelProtocol.OpenAiChat, "http://127.0.0.1:11434/v1", "qwen3:8b", false),
        preset("lm-studio", "LM Studio", ProviderCategory.Local, ModelProtocol.OpenAiChat, "http://127.0.0.1:1234/v1", "local-model", false),
        preset("vllm", "vLLM", ProviderCategory.Local, ModelProtocol.OpenAiChat, "http://127.0.0.1:8000/v1", "local-model", false),
        preset("localai", "LocalAI", ProviderCategory.Local, ModelProtocol.OpenAiChat, "http://127.0.0.1:8080/v1", "local-model", false),
        preset("litellm", "LiteLLM Proxy", ProviderCategory.Local, ModelProtocol.OpenAiChat, "http://127.0.0.1:4000/v1", "model-alias", false),
        preset("newapi", "NewAPI / OneAPI", ProviderCategory.Custom, ModelProtocol.OpenAiChat, "", "model-id"),
        preset("custom-openai", "Custom · OpenAI compatible", ProviderCategory.Custom, ModelProtocol.OpenAiChat, "", "model-id"),
        preset("custom-responses", "Custom · OpenAI Responses", ProviderCategory.Custom, ModelProtocol.OpenAiResponses, "", "model-id"),
        preset("custom-anthropic", "Custom · Anthropic compatible", ProviderCategory.Custom, ModelProtocol.AnthropicMessages, "", "model-id"),
        preset("custom-gemini", "Custom · Gemini compatible", ProviderCategory.Custom, ModelProtocol.GoogleGemini, "", "model-id"),
    )

    private fun preset(
        id: String,
        name: String,
        category: ProviderCategory,
        protocol: ModelProtocol,
        baseUrl: String,
        exampleModel: String,
        requiresApiKey: Boolean = true,
    ) = ProviderPreset(id, name, category, protocol, baseUrl, exampleModel, requiresApiKey)
}
