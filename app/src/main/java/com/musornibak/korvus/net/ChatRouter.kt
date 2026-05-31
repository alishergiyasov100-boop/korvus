package com.musornibak.korvus.net

import com.musornibak.korvus.data.model.Message
import com.musornibak.korvus.data.model.ModelInfo
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.data.model.Provider

class ChatRouter(
    private val hfToken: String,
    private val autoFailover: Boolean
) {

    private val hf = HfClient(hfToken)
    private val poll = PollinationsClient()

    suspend fun ask(
        model: ModelInfo,
        history: List<Message>,
        systemPrompt: String
    ): RouterResult {
        val attempts = if (autoFailover) failoverChain(model) else listOf(model)
        var lastError: Throwable? = null
        for (attempt in attempts) {
            try {
                val text = when (attempt.provider) {
                    Provider.HF -> hf.chat(attempt.providerModelId, history, systemPrompt)
                    Provider.POLLINATIONS -> poll.chat(attempt.providerModelId, history, systemPrompt)
                }
                return RouterResult(content = text, usedModel = attempt, fallbackFrom = if (attempt.id != model.id) model.id else null)
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: RuntimeException("Все провайдеры лежат")
    }

    private fun failoverChain(primary: ModelInfo): List<ModelInfo> {
        val chain = mutableListOf(primary)
        if (primary.provider == Provider.HF) {
            // primary HF → other HF model of same category → pollinations fallback
            val sameCat = ModelRegistry.ALL.firstOrNull {
                it.provider == Provider.HF && it.id != primary.id
            }
            if (sameCat != null) chain.add(sameCat)
            val pollFallback = ModelRegistry.ALL.firstOrNull { it.provider == Provider.POLLINATIONS }
            if (pollFallback != null) chain.add(pollFallback)
        } else {
            // primary Pollinations → other Pollinations → HF Qwen3
            val otherPoll = ModelRegistry.ALL.firstOrNull {
                it.provider == Provider.POLLINATIONS && it.id != primary.id
            }
            if (otherPoll != null) chain.add(otherPoll)
            val hfFallback = ModelRegistry.byId(ModelRegistry.DEFAULT_ID)
            chain.add(hfFallback)
        }
        return chain.distinctBy { it.id }
    }
}

data class RouterResult(
    val content: String,
    val usedModel: ModelInfo,
    val fallbackFrom: String?
)
