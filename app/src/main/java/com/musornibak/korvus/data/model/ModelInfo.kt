package com.musornibak.korvus.data.model

enum class Provider { SILICONFLOW }

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
            id = "sf-qwen3-coder-480b",
            displayName = "Qwen3-Coder 480B",
            provider = Provider.SILICONFLOW,
            providerModelId = "Qwen/Qwen3-Coder-480B-A35B-Instruct",
            emoji = "\uD83D\uDEE0",
            tagline = "Free · SiliconFlow",
            logoUrl = "https://cdn.simpleicons.org/qwen/D97757"
        )
    )

    val DEFAULT_ID = "sf-qwen3-coder-480b"

    fun byId(id: String): ModelInfo = ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }
}
