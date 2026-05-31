package com.musornibak.korvus.net

import com.musornibak.korvus.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PollinationsClient {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chat(
        providerModelId: String,
        messages: List<Message>,
        systemPrompt: String,
        maxTokens: Int = 2048,
        temperature: Double = 0.7
    ): String = withContext(Dispatchers.IO) {
        val msgArray: JsonArray = buildJsonArray {
            add(buildJsonObject {
                put("role", "system")
                put("content", systemPrompt)
            })
            messages.forEach { m ->
                add(buildJsonObject {
                    put("role", m.role)
                    put("content", m.content)
                })
            }
        }

        val body: JsonObject = buildJsonObject {
            put("model", providerModelId)
            put("messages", msgArray)
            put("max_tokens", maxTokens)
            put("temperature", temperature)
            put("stream", false)
            put("private", true)
            put("referrer", "korvus-android")
        }

        val req = Request.Builder()
            .url("https://text.pollinations.ai/openai/chat/completions")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        HttpClient.instance.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: throw RuntimeException("Пустой ответ от Pollinations")
            if (!resp.isSuccessful) {
                throw RuntimeException("Pollinations ${resp.code}: ${raw.take(400)}")
            }
            val parsed = json.parseToJsonElement(raw).jsonObject
            val choices = parsed["choices"]?.jsonArray ?: throw RuntimeException("Нет choices")
            val first = choices.firstOrNull()?.jsonObject ?: throw RuntimeException("choices пуст")
            val message = first["message"]?.jsonObject ?: throw RuntimeException("Нет message")
            message["content"]?.jsonPrimitive?.content ?: ""
        }
    }
}
