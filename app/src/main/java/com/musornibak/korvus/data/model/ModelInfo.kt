package com.musornibak.korvus.data.model

enum class Provider { HF, POLLINATIONS, COMPLETIONS }

data class ModelInfo(
    val id: String,
    val displayName: String,
    val provider: Provider,
    val providerModelId: String,
    val emoji: String,
    val tagline: String
)

object ModelRegistry {
    val ALL: List<ModelInfo> = listOf(
        ModelInfo(
            id = "qwen3-coder-480b",
            displayName = "Qwen3-Coder 480B",
            provider = Provider.HF,
            providerModelId = "Qwen/Qwen3-Coder-480B-A35B-Instruct",
            emoji = "\uD83D\uDEE0",
            tagline = "Код, агент"
        ),
        ModelInfo(
            id = "deepseek-v3-2",
            displayName = "DeepSeek V3.2",
            provider = Provider.HF,
            providerModelId = "deepseek-ai/DeepSeek-V3.2-Exp",
            emoji = "\uD83E\uDDE0",
            tagline = "Рассуждение"
        ),
        ModelInfo(
            id = "llama-4-maverick",
            displayName = "Llama 4 Maverick",
            provider = Provider.HF,
            providerModelId = "meta-llama/Llama-4-Maverick-17B-128E-Instruct",
            emoji = "\uD83D\uDC41",
            tagline = "Vision"
        ),
        ModelInfo(
            id = "kimi-k2",
            displayName = "Kimi K2",
            provider = Provider.HF,
            providerModelId = "moonshotai/Kimi-K2-Instruct",
            emoji = "\u26A1",
            tagline = "Быстрый"
        ),
        ModelInfo(
            id = "cp-claude-opus-4-6",
            displayName = "Claude Opus 4.6",
            provider = Provider.COMPLETIONS,
            providerModelId = "claude-opus-4-6",
            emoji = "\uD83D\uDC51",
            tagline = "Frontier · free"
        ),
        ModelInfo(
            id = "cp-claude-sonnet-4-6",
            displayName = "Claude Sonnet 4.6",
            provider = Provider.COMPLETIONS,
            providerModelId = "claude-sonnet-4-6",
            emoji = "\uD83D\uDC51",
            tagline = "Быстрый Sonnet · free"
        ),
        ModelInfo(
            id = "cp-gpt-5-2",
            displayName = "GPT-5.2",
            provider = Provider.COMPLETIONS,
            providerModelId = "gpt-5.2",
            emoji = "\uD83D\uDE80",
            tagline = "OpenAI · free"
        ),
        ModelInfo(
            id = "cp-gemini-3-pro",
            displayName = "Gemini 3.1 Pro",
            provider = Provider.COMPLETIONS,
            providerModelId = "gemini-3.1-pro",
            emoji = "\u2728",
            tagline = "Google · free"
        ),
        ModelInfo(
            id = "poll-openai-fast",
            displayName = "GPT-OSS 20B",
            provider = Provider.POLLINATIONS,
            providerModelId = "openai-fast",
            emoji = "\u26A1",
            tagline = "Анонимный фолбэк"
        )
    )

    val DEFAULT_ID = "qwen3-coder-480b"

    fun byId(id: String): ModelInfo = ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }
}
