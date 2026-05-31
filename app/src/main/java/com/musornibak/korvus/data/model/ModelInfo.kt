package com.musornibak.korvus.data.model

enum class Provider { HF, POLLINATIONS, COMPLETIONS }

data class ModelInfo(
    val id: String,
    val displayName: String,
    val provider: Provider,
    val providerModelId: String,
    val emoji: String,
    val tagline: String,
    val logoUrl: String? = null
)

object ModelRegistry {
    val ALL: List<ModelInfo> = listOf(
        ModelInfo(
            id = "qwen3-coder-480b",
            displayName = "Qwen3-Coder 480B",
            provider = Provider.HF,
            providerModelId = "Qwen/Qwen3-Coder-480B-A35B-Instruct",
            emoji = "\uD83D\uDEE0",
            tagline = "Код, агент",
            logoUrl = "https://cdn.simpleicons.org/qwen/D97757"
        ),
        ModelInfo(
            id = "deepseek-v3-2",
            displayName = "DeepSeek V3.2",
            provider = Provider.HF,
            providerModelId = "deepseek-ai/DeepSeek-V3.2-Exp",
            emoji = "\uD83E\uDDE0",
            tagline = "Рассуждение",
            logoUrl = "https://cdn.simpleicons.org/deepseek/4D6BFE"
        ),
        ModelInfo(
            id = "llama-4-maverick",
            displayName = "Llama 4 Maverick",
            provider = Provider.HF,
            providerModelId = "meta-llama/Llama-4-Maverick-17B-128E-Instruct",
            emoji = "\uD83D\uDC41",
            tagline = "Vision",
            logoUrl = "https://cdn.simpleicons.org/meta/0467DF"
        ),
        ModelInfo(
            id = "kimi-k2",
            displayName = "Kimi K2",
            provider = Provider.HF,
            providerModelId = "moonshotai/Kimi-K2-Instruct",
            emoji = "\u26A1",
            tagline = "Быстрый",
            logoUrl = "https://cdn.simpleicons.org/moonshot/000000"
        ),
        ModelInfo(
            id = "cp-claude-opus-4-6",
            displayName = "Claude Opus 4.6",
            provider = Provider.COMPLETIONS,
            providerModelId = "claude-opus-4-6",
            emoji = "\uD83D\uDC51",
            tagline = "Frontier · free",
            logoUrl = "https://cdn.simpleicons.org/anthropic/D97757"
        ),
        ModelInfo(
            id = "cp-claude-sonnet-4-6",
            displayName = "Claude Sonnet 4.6",
            provider = Provider.COMPLETIONS,
            providerModelId = "claude-sonnet-4-6",
            emoji = "\uD83D\uDC51",
            tagline = "Быстрый Sonnet · free",
            logoUrl = "https://cdn.simpleicons.org/anthropic/E89B7F"
        ),
        ModelInfo(
            id = "cp-gpt-5-2",
            displayName = "GPT-5.2",
            provider = Provider.COMPLETIONS,
            providerModelId = "gpt-5.2",
            emoji = "\uD83D\uDE80",
            tagline = "OpenAI · free",
            logoUrl = "https://cdn.simpleicons.org/openai/EDE5DC"
        ),
        ModelInfo(
            id = "cp-gemini-3-pro",
            displayName = "Gemini 3.1 Pro",
            provider = Provider.COMPLETIONS,
            providerModelId = "gemini-3.1-pro",
            emoji = "\u2728",
            tagline = "Google · free",
            logoUrl = "https://cdn.simpleicons.org/googlegemini/4285F4"
        ),
        ModelInfo(
            id = "poll-openai-fast",
            displayName = "GPT-OSS 20B",
            provider = Provider.POLLINATIONS,
            providerModelId = "openai-fast",
            emoji = "\u26A1",
            tagline = "Анонимный фолбэк",
            logoUrl = "https://cdn.simpleicons.org/openai/B8AEA1"
        )
    )

    val DEFAULT_ID = "cp-claude-opus-4-6"

    fun byId(id: String): ModelInfo = ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }
}
