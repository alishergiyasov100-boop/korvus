package com.musornibak.korvus.tools

import android.content.Context
import android.util.Base64
import com.musornibak.korvus.net.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

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
            "edit_file" -> {
                val path = call.args["path"]?.jsonPrimitive?.content ?: return "ERROR: missing path"
                val search = call.args["search"]?.jsonPrimitive?.content ?: return "ERROR: missing search"
                val replace = call.args["replace"]?.jsonPrimitive?.content ?: ""
                val origRaw = TermuxBridge.runCmd(ctx, "cat ${quote(path)}", 15_000)
                if (!origRaw.contains(search)) {
                    return "ERROR: search string not found in $path"
                }
                val updated = origRaw.replaceFirst(search, replace)
                val b64 = Base64.encodeToString(updated.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                val cmd = "echo $b64 | base64 -d > ${quote(path)} && echo \"OK: ${path} обновлён\""
                TermuxBridge.runCmd(ctx, cmd, 20_000)
            }
            "list_dir" -> {
                val path = call.args["path"]?.jsonPrimitive?.content ?: return "ERROR: missing path"
                TermuxBridge.runCmd(ctx, "ls -lah ${quote(path)}", 10_000)
            }
            "glob" -> {
                val root = call.args["root"]?.jsonPrimitive?.content ?: "/data/data/com.termux/files/home"
                val pattern = call.args["pattern"]?.jsonPrimitive?.content ?: return "ERROR: missing pattern"
                TermuxBridge.runCmd(ctx, "find ${quote(root)} -name ${quote(pattern)} | head -100", 15_000)
            }
            "run_shell" -> {
                val cmd = call.args["cmd"]?.jsonPrimitive?.content ?: return "ERROR: missing cmd"
                val timeout = (call.args["timeout_ms"]?.jsonPrimitive?.content?.toLongOrNull()) ?: 60_000L
                TermuxBridge.runCmd(ctx, cmd, timeout)
            }
            "web_search" -> {
                val q = call.args["query"]?.jsonPrimitive?.content ?: return "ERROR: missing query"
                webSearch(q)
            }
            "task_create" -> {
                val title = call.args["title"]?.jsonPrimitive?.content ?: return "ERROR: missing title"
                val status = call.args["status"]?.jsonPrimitive?.content ?: "pending"
                val t = AgenticTasks.create(title, status)
                "OK: ${t.id} «${t.title}» [${t.status}]"
            }
            "task_update" -> {
                val id = call.args["id"]?.jsonPrimitive?.content ?: return "ERROR: missing id"
                val status = call.args["status"]?.jsonPrimitive?.content
                val title = call.args["title"]?.jsonPrimitive?.content
                val t = AgenticTasks.update(id, status = status, title = title)
                    ?: return "ERROR: task $id not found"
                "OK: ${t.id} «${t.title}» [${t.status}]"
            }
            "task_list" -> {
                val ts = AgenticTasks.tasks.value
                if (ts.isEmpty()) "(no tasks)"
                else ts.joinToString("\n") { "${it.id} [${it.status}] ${it.title}" }
            }
            else -> "ERROR: unknown tool '${call.name}'"
        }
    }

    private suspend fun webSearch(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("https://html.duckduckgo.com/html/?q=$encoded")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
                .build()
            HttpClient.instance.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use "ERROR: search ${resp.code}"
                val html = resp.body?.string() ?: return@use "ERROR: empty body"
                val results = mutableListOf<String>()
                val resultRe = Regex(
                    "<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>[\\s\\S]*?<a[^>]+class=\"result__snippet\"[^>]*>([\\s\\S]*?)</a>",
                    RegexOption.IGNORE_CASE
                )
                for (m in resultRe.findAll(html).take(8)) {
                    val rawUrl = m.groupValues[1]
                    val url = unwrapDdg(rawUrl)
                    val title = stripTags(m.groupValues[2]).trim()
                    val snippet = stripTags(m.groupValues[3]).trim()
                    results.add("• $title\n  $url\n  $snippet")
                }
                if (results.isEmpty()) "(нет результатов для «$query»)" else results.joinToString("\n\n")
            }
        } catch (t: Throwable) {
            "ERROR: ${t.message}"
        }
    }

    private fun unwrapDdg(raw: String): String {
        val v = if (raw.startsWith("//")) "https:$raw" else raw
        val m = Regex("uddg=([^&]+)").find(v) ?: return v
        return java.net.URLDecoder.decode(m.groupValues[1], "UTF-8")
    }

    private fun stripTags(s: String): String =
        s.replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")

    private fun quote(s: String): String {
        // single-quote for shell, escape embedded single quotes
        return "'" + s.replace("'", "'\\''") + "'"
    }

    const val SYSTEM_TOOL_PROMPT = """
У тебя есть tools для работы с устройством Алишера и интернетом. Когда нужно использовать tool — выведи ОДИН ```tool блок в формате JSON:

```tool
{"name":"run_shell","args":{"cmd":"ls /sdcard/Download"}}
```

Доступные tools:
- read_file: {"path":"/абс/путь"} — читает файл
- write_file: {"path":"/абс/путь","content":"..."} — пишет файл (создаёт директории)
- edit_file: {"path":"/абс/путь","search":"старый текст","replace":"новый"} — search/replace в существующем файле
- list_dir: {"path":"/абс/путь"} — ls -lah
- glob: {"root":"/абс/путь","pattern":"*.kt"} — find по имени, до 100 результатов
- run_shell: {"cmd":"...", "timeout_ms":30000} — bash -c "...", вернёт stdout+stderr
- web_search: {"query":"..."} — поиск в DuckDuckGo, до 8 результатов с url + сниппет
- task_create: {"title":"...", "status":"pending|in_progress|done"} — создать таску в панели
- task_update: {"id":"t1","status":"done","title":"..."} — обновить таску
- task_list: {} — показать все таски
- agent: {"prompt":"...","role":"исследователь|кодер|..."} — спавнит sub-agent с теми же tools (кроме вложенных agent). Возвращает короткий ответ.

Правила:
1. Используй ТОЛЬКО один ```tool блок за раз. Между tool-вызовами я верну тебе результат, и ты можешь дальше думать или вызывать следующий tool.
2. Для сложных задач сразу разбей их на task_create (как Claude Code), помечай done через task_update.
3. Если хочешь просто ответить — пиши обычным текстом без ```tool блоков.
4. Все пути — абсолютные. Termux work dir: /data/data/com.termux/files/home
5. Пользовательские файлы обычно в /sdcard/Download/ или /sdcard/Pictures/
"""
}
