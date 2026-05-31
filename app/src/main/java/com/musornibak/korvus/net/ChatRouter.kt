package com.musornibak.korvus.net

import com.musornibak.korvus.data.model.Message
import com.musornibak.korvus.data.model.ModelInfo
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.data.model.Provider

class ChatRouter(
    private val hfToken: String,
    private val completionsToken: String,
    private val autoFailover: Boolean
) {

    private val hf = HfClient(hfToken)
    private val poll = PollinationsClient()
    private val cp = CompletionsClient(completionsToken)

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
                    Provider.COMPLETIONS -> cp.chat(attempt.providerModelId, history, systemPrompt)
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
        val cpFallback = ModelRegistry.ALL.firstOrNull { it.provider == Provider.COMPLETIONS && it.id != primary.id }
        val pollFallback = ModelRegistry.ALL.firstOrNull { it.provider == Provider.POLLINATIONS && it.id != primary.id }
        val hfFallback = ModelRegistry.ALL.firstOrNull { it.provider == Provider.HF && it.id != primary.id }
            ?: ModelRegistry.byId(ModelRegistry.DEFAULT_ID)

        when (primary.provider) {
            Provider.HF -> {
                ModelRegistry.ALL.firstOrNull { it.provider == Provider.HF && it.id != primary.id }?.let { chain.add(it) }
                cpFallback?.let { chain.add(it) }
                pollFallback?.let { chain.add(it) }
            }
            Provider.COMPLETIONS -> {
                cpFallback?.let { chain.add(it) }
                if (chain.last().id == primary.id) Unit
                chain.add(hfFallback)
                pollFallback?.let { chain.add(it) }
            }
            Provider.POLLINATIONS -> {
                pollFallback?.let { chain.add(it) }
                cpFallback?.let { chain.add(it) }
                chain.add(hfFallback)
            }
        }
        return chain.distinctBy { it.id }
    }
}

data class RouterResult(
    val content: String,
    val usedModel: ModelInfo,
    val fallbackFrom: String?
)
