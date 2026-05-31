package com.musornibak.korvus.tools

import android.content.Context
import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ToolCall(val name: String, val args: JsonObject, val rawBlock: String)

object ToolRuntime {

    private val json = Json { ignoreUnknownKeys = true }
    private val FENCE_RE = Regex(
        "```\\s*tool\\s*\\n([\\s\\S]*?)\\n```",
        RegexOption.IGNORE_CASE
    )

    fun parseToolCalls(text: String): List<ToolCall> {
        val out = mutableListOf<ToolCall>()
        for (m in FENCE_RE.findAll(text)) {
            val body = m.groupValues[1].trim()
            val parsed: JsonElement = try { json.parseToJsonElement(body) } catch (_: Throwable) { continue }
            val obj = parsed as? JsonObject ?: continue
            val name = obj["name"]?.jsonPrimitive?.content ?: continue
            val args = (obj["args"] as? JsonObject) ?: JsonObject(emptyMap())
            out.add(ToolCall(name = name, args = args, rawBlock = m.value))
        }
        return out
    }

    suspend fun execute(ctx: Context, call: ToolCall): String {
        return when (call.name) {
            "read_file" -> {
                val path = call.args["path"]?.jsonPrimitive?.content ?: return "ERROR: missing path"
                TermuxBridge.runCmd(ctx, "cat ${quote(path)}", 15_000)
            }
            "write_file" -> {
                val path = call.args["path"]?.jsonPrimitive?.content ?: return "ERROR: missing path"
                val content = call.args["content"]?.jsonPrimitive?.content ?: return "ERROR: missing content"
                val b64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                val cmd = "mkdir -p \"\$(dirname ${quote(path)})\" && echo $b64 | base64 -d > ${quote(path)} && echo \"OK: записано \$(wc -c < ${quote(path)}) байт в $path\""
                TermuxBridge.runCmd(ctx, cmd, 20_000)
            }
            "list_dir" -> {
                val path = call.args["path"]?.jsonPrimitive?.content ?: return "ERROR: missing path"
                TermuxBridge.runCmd(ctx, "ls -lah ${quote(path)}", 10_000)
            }
            "run_shell" -> {
                val cmd = call.args["cmd"]?.jsonPrimitive?.content ?: return "ERROR: missing cmd"
                val timeout = (call.args["timeout_ms"]?.jsonPrimitive?.content?.toLongOrNull()) ?: 60_000L
                TermuxBridge.runCmd(ctx, cmd, timeout)
            }
            else -> "ERROR: unknown tool '${call.name}'"
        }
    }

    private fun quote(s: String): String {
        // single-quote for shell, escape embedded single quotes
        return "'" + s.replace("'", "'\\''") + "'"
    }

    const val SYSTEM_TOOL_PROMPT = """
У тебя есть tools для работы с устройством Алишера через Termux. Когда нужно использовать tool — выведи ОДИН ```tool блок в формате JSON:

```tool
{"name":"run_shell","args":{"cmd":"ls /sdcard/Download"}}
```

Доступные tools:
- read_file: {"path":"/абсолютный/путь"} — читает файл и возвращает содержимое
- write_file: {"path":"/абс/путь","content":"..."} — пишет файл (создаёт директории при необходимости)
- list_dir: {"path":"/абс/путь"} — ls -lah
- run_shell: {"cmd":"...", "timeout_ms":30000} — bash -c "...", вернёт stdout+stderr

Правила:
1. Используй ТОЛЬКО один ```tool блок за раз. Между tool-вызовами я верну тебе результат, и ты можешь дальше думать или вызывать следующий tool.
2. Если хочешь просто ответить пользователю — пиши обычным текстом без ```tool блоков.
3. Все пути — абсолютные. Termux work dir: /data/data/com.termux/files/home
4. Пользовательские файлы обычно в /sdcard/Download/ или /sdcard/Pictures/
"""
}
