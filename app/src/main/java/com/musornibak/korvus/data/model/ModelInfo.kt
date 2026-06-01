package com.musornibak.korvus.data.model

import kotlinx.serialization.Serializable

enum class Provider { DEEPSEEK_PROXY, OPENAI_COMPAT }

data class ModelInfo(
    val id: String,
    val displayName: String,
    val provider: Provider,
    val providerModelId: String,
    val emoji: String,
    val tagline: String,
    val logoUrl: String? = null,
    val isCustom: Boolean = false,
    val customBaseUrl: String? = null,
    val customApiKey: String? = null
)

@Serializable
data class CustomModel(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val apiKey: String,
    val providerModelId: String,
    val logoUrl: String? = null,
    val tagline: String = "Custom"
) {
    fun toInfo(): ModelInfo = ModelInfo(
        id = id,
        displayName = displayName,
        provider = Provider.OPENAI_COMPAT,
        providerModelId = providerModelId,
        emoji = "",
        tagline = tagline,
        logoUrl = logoUrl,
        isCustom = true,
        customBaseUrl = baseUrl,
        customApiKey = apiKey
    )
}

object ModelRegistry {
    val BUILT_IN: List<ModelInfo> = listOf(
        ModelInfo(
            id = "deepseek-v4-flash",
            displayName = "DeepSeek V4 Flash",
            provider = Provider.DEEPSEEK_PROXY,
            providerModelId = "deepseek-v4-flash",
            emoji = "\uD83C\uDF0A",
            tagline = "Free · local proxy",
            logoUrl = "https://cdn.simpleicons.org/deepseek/4D6BFE"
        ),
        ModelInfo(
            id = "deepseek-v4-pro",
            displayName = "DeepSeek V4 Pro",
            provider = Provider.DEEPSEEK_PROXY,
            providerModelId = "deepseek-v4-pro",
            emoji = "\u2728",
            tagline = "Expert · local proxy",
            logoUrl = "https://cdn.simpleicons.org/deepseek/4D6BFE"
        )
    )

    const val DEFAULT_ID = "deepseek-v4-flash"

    @Volatile
    private var customCache: List<ModelInfo> = emptyList()

    fun setCustom(list: List<CustomModel>) {
        customCache = list.map { it.toInfo() }
    }

    fun all(): List<ModelInfo> = BUILT_IN + customCache

    fun byId(id: String): ModelInfo =
        all().firstOrNull { it.id == id } ?: BUILT_IN.first { it.id == DEFAULT_ID }
}
