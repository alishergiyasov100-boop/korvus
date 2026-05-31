package com.musornibak.korvus.data.model

enum class Provider { HF, POLLINATIONS }

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
            id = "claude-opus-4-8",
            displayName = "Claude Opus 4.8",
            provider = Provider.POLLINATIONS,
            providerModelId = "claude",
            emoji = "\uD83D\uDC51",
            tagline = "Frontier"
        ),
        ModelInfo(
            id = "gpt-5-2",
            displayName = "GPT-5.2",
            provider = Provider.POLLINATIONS,
            providerModelId = "openai-large",
            emoji = "\uD83D\uDE80",
            tagline = "Универсал"
        ),
        ModelInfo(
            id = "o5-reasoning",
            displayName = "o5 Reasoning",
            provider = Provider.POLLINATIONS,
            providerModelId = "openai-reasoning",
            emoji = "\uD83D\uDD2C",
            tagline = "Глубокий thinking"
        )
    )

    val DEFAULT_ID = "qwen3-coder-480b"

    fun byId(id: String): ModelInfo = ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }
}
