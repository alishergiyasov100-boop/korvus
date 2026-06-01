package com.musornibak.korvus.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musornibak.korvus.data.model.Message
import com.musornibak.korvus.data.model.ModelInfo
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.data.prefs.UserPrefs
import com.musornibak.korvus.data.store.ThreadInfo
import com.musornibak.korvus.data.store.ThreadStore
import com.musornibak.korvus.net.OpenAIClient
import com.musornibak.korvus.tools.ToolCall
import com.musornibak.korvus.tools.ToolRuntime
import kotlinx.serialization.json.jsonPrimitive
import com.musornibak.korvus.widget.KorvusWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = UserPrefs(app)
    private val store = ThreadStore(app)

    val messages: StateFlow<List<Message>> = store.messages
    val threads: StateFlow<List<ThreadInfo>> = store.threads
    val activeThreadId: StateFlow<String?> = store.activeId

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _statusLine = MutableStateFlow<String?>(null)
    val statusLine: StateFlow<String?> = _statusLine.asStateFlow()

    private val _streamingContent = MutableStateFlow<String?>(null)
    val streamingContent: StateFlow<String?> = _streamingContent.asStateFlow()

    val selectedModelId: StateFlow<String> = run {
        val f = MutableStateFlow(ModelRegistry.DEFAULT_ID)
        viewModelScope.launch { prefs.selectedModelId.collect { f.value = it } }
        f.asStateFlow()
    }

    val availableModels: StateFlow<List<ModelInfo>> = run {
        val f = MutableStateFlow(ModelRegistry.all())
        viewModelScope.launch {
            prefs.customModels.collect { customs ->
                ModelRegistry.setCustom(customs)
                f.value = ModelRegistry.all()
            }
        }
        f.asStateFlow()
    }

    init {
        viewModelScope.launch { store.ensureInitial() }
    }

    fun selectModel(id: String) {
        prefs.setSelectedModel(id)
    }

    fun newThread() {
        viewModelScope.launch {
            store.createThread()
            refreshWidget()
        }
    }

    fun selectThread(id: String) {
        viewModelScope.launch {
            store.setActive(id)
            refreshWidget()
        }
    }

    fun deleteThread(id: String) {
        viewModelScope.launch {
            store.deleteThread(id)
            refreshWidget()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            store.clearActive()
            refreshWidget()
        }
    }

    fun send(userText: String, userName: String) {
        if (userText.isBlank() || _isSending.value) return
        val text = userText.trim()
        viewModelScope.launch {
            _isSending.value = true
            try {
                store.ensureInitial()
                store.appendActive(Message(role = "user", content = text))
                refreshWidget()

                val modelId = prefs.selectedModelId.first()
                val token = prefs.proxyApiKey.first()
                val baseUrl = prefs.proxyBaseUrl.first()
                val model = ModelRegistry.byId(modelId)
                val effectiveBase = model.customBaseUrl?.takeIf { it.isNotBlank() } ?: baseUrl
                val effectiveKey = model.customApiKey?.takeIf { it.isNotBlank() } ?: token
                val client = OpenAIClient(effectiveBase, effectiveKey)
                val systemPrompt = buildSystemPrompt(userName)
                runAgenticLoop(client, model, systemPrompt)
            } catch (t: Throwable) {
                _streamingContent.value = null
                store.appendActive(Message(role = "assistant", content = "⚠️ Ошибка: ${t.message ?: t::class.simpleName}"))
                refreshWidget()
            } finally {
                _isSending.value = false
                _statusLine.value = null
                _streamingContent.value = null
            }
        }
    }

    private suspend fun runAgenticLoop(
        client: OpenAIClient,
        model: ModelInfo,
        systemPrompt: String,
        maxIterations: Int = 6
    ) {
        repeat(maxIterations) { iteration ->
            _statusLine.value = if (iteration == 0) "Thinking" else "Tool round ${iteration}"
            _streamingContent.value = ""
            val history = messages.value

            val finalText = client.chatStream(
                providerModelId = model.providerModelId,
                messages = history,
                systemPrompt = systemPrompt
            ) { delta ->
                _streamingContent.value = (_streamingContent.value ?: "") + delta
            }

            _streamingContent.value = null
            store.appendActive(Message(role = "assistant", content = finalText, modelId = model.id))
            refreshWidget()

            val calls = ToolRuntime.parseToolCalls(finalText)
            if (calls.isEmpty()) return
            val call = calls.first()
            _statusLine.value = "Running ${call.name}"
            val outRaw = try {
                if (call.name == "agent") runSubAgent(client, model, call) else ToolRuntime.execute(getApplication(), call)
            } catch (t: Throwable) {
                "ERROR: ${t.message}"
            }
            val outClipped = if (outRaw.length > 8000) outRaw.take(8000) + "\n…[обрезано до 8000]" else outRaw
            store.appendActive(Message(role = "user", content = "[tool result: ${call.name}]\n$outClipped"))
            refreshWidget()
        }
        store.appendActive(Message(role = "assistant", content = "⚠ Цикл инструментов остановлен (лимит $maxIterations)."))
        refreshWidget()
    }

    private suspend fun runSubAgent(
        client: OpenAIClient,
        model: ModelInfo,
        call: ToolCall,
        maxIterations: Int = 4
    ): String {
        val prompt = call.args["prompt"]?.jsonPrimitive?.content
            ?: return "ERROR: sub-agent missing 'prompt'"
        val role = call.args["role"]?.jsonPrimitive?.content ?: "помощник-исследователь"
        val subSystem = """
ты sub-agent MiaMuy в роли «$role».

Тебе дали задачу. Используй tools (read_file, write_file, edit_file, list_dir, glob, run_shell, web_search, task_create, task_update, task_list) чтобы её решить.

Когда задача решена — выведи короткий итоговый ответ в обычном тексте без ```tool блоков. Этот ответ вернётся главному агенту.

${ToolRuntime.SYSTEM_TOOL_PROMPT}
        """.trimIndent()
        val sub = mutableListOf(Message(role = "user", content = prompt))
        var last = ""
        repeat(maxIterations) {
            val text = client.chatStream(
                providerModelId = model.providerModelId,
                messages = sub,
                systemPrompt = subSystem
            ) { /* no streaming up */ }
            sub.add(Message(role = "assistant", content = text))
            last = text
            val sc = ToolRuntime.parseToolCalls(text)
            if (sc.isEmpty()) return "[sub-agent]\n$text"
            val sub1 = sc.first()
            if (sub1.name == "agent") {
                sub.add(Message(role = "user", content = "[tool result: agent]\nERROR: nested sub-agents disabled"))
                return@repeat
            }
            val out = try { ToolRuntime.execute(getApplication(), sub1) } catch (t: Throwable) { "ERROR: ${t.message}" }
            val clipped = if (out.length > 4000) out.take(4000) + "\n…" else out
            sub.add(Message(role = "user", content = "[tool result: ${sub1.name}]\n$clipped"))
        }
        return "[sub-agent · лимит]\n$last"
    }

    private fun buildSystemPrompt(userName: String): String {
        return """
ты компаньон по устройству Алишера.

Твоё имя — MiaMuy. Имя пользователя — $userName. Обращайся к нему по имени когда уместно.

${ToolRuntime.SYSTEM_TOOL_PROMPT}
""".trimIndent()
    }

    private fun refreshWidget() {
        KorvusWidgetProvider.requestUpdate(getApplication())
    }
}
