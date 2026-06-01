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
import java.io.BufferedReader
import java.io.InputStreamReader

class OpenAIClient(
    private val baseUrl: String,
    private val apiKey: String
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chatStream(
        providerModelId: String,
        messages: List<Message>,
        systemPrompt: String,
        maxTokens: Int? = null,
        temperature: Double = 0.6,
        onDelta: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw RuntimeException("Нет API ключа. Зайди в Настройки и создай ключ в админке ds-free-api.")
        if (baseUrl.isBlank()) throw RuntimeException("Нет адреса бэкенда.")

        val msgArray: JsonArray = buildJsonArray {
            add(buildJsonObject {
                put("role", "system")
                put("content", systemPrompt)
            })
            messages.forEach { m ->
                val role = when {
                    m.role == "user" && m.content.startsWith("[tool result:") -> "user"
                    m.role == "assistant" -> "assistant"
                    else -> m.role
                }
                add(buildJsonObject {
                    put("role", role)
                    put("content", m.content)
                })
            }
        }

        val body: JsonObject = buildJsonObject {
            put("model", providerModelId)
            put("messages", msgArray)
            if (maxTokens != null) put("max_tokens", maxTokens)
            put("temperature", temperature)
            put("stream", true)
        }

        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .build()

        val sb = StringBuilder()
        HttpClient.instance.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val errBody = try { resp.body?.string()?.take(500) } catch (_: Throwable) { null }
                throw RuntimeException("Backend ${resp.code}: ${errBody ?: "no body"}")
            }
            val source = resp.body?.byteStream() ?: throw RuntimeException("Пустой стрим")
            val reader = BufferedReader(InputStreamReader(source, Charsets.UTF_8))
            reader.useLines { lines ->
                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.isEmpty() || !line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    val chunk = try {
                        json.parseToJsonElement(payload).jsonObject
                    } catch (_: Throwable) { continue }
                    val choices = chunk["choices"]?.jsonArray ?: continue
                    val first = choices.firstOrNull()?.jsonObject ?: continue
                    val delta = first["delta"]?.jsonObject ?: continue
                    val text = delta["content"]?.jsonPrimitive?.content ?: continue
                    if (text.isNotEmpty()) {
                        sb.append(text)
                        onDelta(text)
                    }
                }
            }
        }
        sb.toString()
    }
}
