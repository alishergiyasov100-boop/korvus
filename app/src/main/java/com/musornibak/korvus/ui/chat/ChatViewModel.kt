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
import com.musornibak.korvus.net.ChatRouter
import com.musornibak.korvus.tools.ToolRuntime
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

    val selectedModelId: StateFlow<String> = run {
        val f = MutableStateFlow(ModelRegistry.DEFAULT_ID)
        viewModelScope.launch { prefs.selectedModelId.collect { f.value = it } }
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
                val token = prefs.hfToken.first()
                val failover = prefs.autoFailover.first()
                val model = ModelRegistry.byId(modelId)
                val router = ChatRouter(hfToken = token, autoFailover = failover)

                val systemPrompt = buildSystemPrompt(userName)
                runAgenticLoop(router, model, systemPrompt)
            } catch (t: Throwable) {
                store.appendActive(Message(role = "assistant", content = "⚠️ Ошибка: ${t.message ?: t::class.simpleName}"))
                refreshWidget()
            } finally {
                _isSending.value = false
                _statusLine.value = null
            }
        }
    }

    private suspend fun runAgenticLoop(
        router: ChatRouter,
        initialModel: ModelInfo,
        systemPrompt: String,
        maxIterations: Int = 6
    ) {
        val model = initialModel
        repeat(maxIterations) { iteration ->
            _statusLine.value = if (iteration == 0) "${model.displayName} думает…" else "Tool round ${iteration}…"
            val history = messages.value
            val result = router.ask(model, history, systemPrompt)
            if (result.fallbackFrom != null) {
                _statusLine.value = "⚠ ${result.fallbackFrom} → ${result.usedModel.displayName}"
            }
            val text = result.content
            store.appendActive(Message(role = "assistant", content = text, modelId = result.usedModel.id))
            refreshWidget()

            val calls = ToolRuntime.parseToolCalls(text)
            if (calls.isEmpty()) return
            val call = calls.first()
            _statusLine.value = "🔧 ${call.name}…"
            val outRaw = try {
                ToolRuntime.execute(getApplication(), call)
            } catch (t: Throwable) {
                "ERROR: ${t.message}"
            }
            val outClipped = if (outRaw.length > 8000) outRaw.take(8000) + "\n…[обрезано до 8000 символов]" else outRaw
            store.appendActive(Message(role = "user", content = "[tool result: ${call.name}]\n$outClipped"))
            refreshWidget()
        }
        store.appendActive(Message(role = "assistant", content = "⚠ Цикл инструментов остановлен (лимит $maxIterations итераций)."))
        refreshWidget()
    }

    private fun buildSystemPrompt(userName: String): String {
        return """
ты компаньон по устройству Алишера.

Твоё имя — Корвус. Имя пользователя — $userName. Обращайся к нему по имени когда уместно.

${ToolRuntime.SYSTEM_TOOL_PROMPT}
""".trimIndent()
    }

    private fun refreshWidget() {
        KorvusWidgetProvider.requestUpdate(getApplication())
    }
}
