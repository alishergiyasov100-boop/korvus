package com.musornibak.korvus.data.model

enum class Provider { DEEPSEEK_PROXY }

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
            id = "deepseek-v4-pro",
            displayName = "DeepSeek V4 Pro",
            provider = Provider.DEEPSEEK_PROXY,
            providerModelId = "deepseek-v4-pro",
            emoji = "\uD83C\uDF0A",
            tagline = "Free · local proxy",
            logoUrl = "https://cdn.simpleicons.org/deepseek/4D6BFE"
        )
    )

    val DEFAULT_ID = "deepseek-v4-pro"

    fun byId(id: String): ModelInfo = ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }
}
