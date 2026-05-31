package com.musornibak.korvus.data.store

import android.content.Context
import com.musornibak.korvus.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class ThreadInfo(
    val id: String,
    val title: String,
    val lastTs: Long,
    val lastSnippet: String
)

class ThreadStore(ctx: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val msgSer = ListSerializer(Message.serializer())
    private val infoSer = ListSerializer(ThreadInfo.serializer())

    private val baseDir = File(ctx.filesDir, "threads").apply { mkdirs() }
    private val indexFile = File(baseDir, "index.json")
    private val activeFile = File(baseDir, "active.txt")
    private val mirrorFile = File(ctx.filesDir, "messages.json")

    private val _threads = MutableStateFlow<List<ThreadInfo>>(loadIndex())
    val threads = _threads.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId = _activeId.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    init {
        val saved = try {
            if (activeFile.exists()) activeFile.readText().trim().takeIf { it.isNotEmpty() } else null
        } catch (_: Throwable) { null }
        if (saved != null && threadFile(saved).exists()) {
            _activeId.value = saved
            _messages.value = loadMessages(saved)
        }
    }

    private fun threadFile(id: String) = File(baseDir, "$id.json")

    private fun loadIndex(): List<ThreadInfo> = try {
        if (indexFile.exists()) json.decodeFromString(infoSer, indexFile.readText()) else emptyList()
    } catch (_: Throwable) { emptyList() }

    private fun loadMessages(id: String): List<Message> = try {
        val f = threadFile(id)
        if (f.exists()) json.decodeFromString(msgSer, f.readText()) else emptyList()
    } catch (_: Throwable) { emptyList() }

    suspend fun ensureInitial(): String = withContext(Dispatchers.IO) {
        val current = _activeId.value
        if (current != null && threadFile(current).exists()) return@withContext current
        val id = _threads.value.firstOrNull()?.id ?: createInternal()
        setActiveInternal(id)
        id
    }

    suspend fun createThread(): String = withContext(Dispatchers.IO) {
        val id = createInternal()
        setActiveInternal(id)
        id
    }

    suspend fun setActive(id: String) = withContext(Dispatchers.IO) {
        if (threadFile(id).exists()) setActiveInternal(id)
    }

    suspend fun appendActive(m: Message) = withContext(Dispatchers.IO) {
        val id = _activeId.value ?: return@withContext
        val next = _messages.value + m
        _messages.value = next
        persistMessages(id, next)
        bumpIndex(id, next)
        mirror(next)
    }

    suspend fun replaceLastActive(m: Message) = withContext(Dispatchers.IO) {
        val id = _activeId.value ?: return@withContext
        val cur = _messages.value.toMutableList()
        if (cur.isNotEmpty()) cur[cur.size - 1] = m
        _messages.value = cur
        persistMessages(id, cur)
        bumpIndex(id, cur)
        mirror(cur)
    }

    suspend fun clearActive() = withContext(Dispatchers.IO) {
        val id = _activeId.value ?: return@withContext
        _messages.value = emptyList()
        persistMessages(id, emptyList())
        bumpIndex(id, emptyList())
        mirror(emptyList())
    }

    suspend fun deleteThread(id: String) = withContext(Dispatchers.IO) {
        try { threadFile(id).delete() } catch (_: Throwable) {}
        val updated = _threads.value.filterNot { it.id == id }
        _threads.value = updated
        persistIndex(updated)
        if (_activeId.value == id) {
            val fallback = updated.firstOrNull()?.id ?: createInternal()
            setActiveInternal(fallback)
        }
    }

    private fun createInternal(): String {
        val id = UUID.randomUUID().toString()
        try { threadFile(id).writeText(json.encodeToString(msgSer, emptyList())) } catch (_: Throwable) {}
        val info = ThreadInfo(id, "Новый чат", System.currentTimeMillis(), "")
        val updated = listOf(info) + _threads.value
        persistIndex(updated)
        _threads.value = updated
        return id
    }

    private fun setActiveInternal(id: String) {
        _activeId.value = id
        val msgs = loadMessages(id)
        _messages.value = msgs
        try { activeFile.writeText(id) } catch (_: Throwable) {}
        mirror(msgs)
    }

    private fun persistMessages(id: String, list: List<Message>) {
        try { threadFile(id).writeText(json.encodeToString(msgSer, list)) } catch (_: Throwable) {}
    }

    private fun mirror(list: List<Message>) {
        try { mirrorFile.writeText(json.encodeToString(msgSer, list)) } catch (_: Throwable) {}
    }

    private fun persistIndex(list: List<ThreadInfo>) {
        try { indexFile.writeText(json.encodeToString(infoSer, list)) } catch (_: Throwable) {}
    }

    private fun bumpIndex(id: String, msgs: List<Message>) {
        val firstUser = msgs.firstOrNull { it.role == "user" && !it.content.startsWith("[tool result:") }
        val lastShown = msgs.lastOrNull { it.role == "assistant" && !it.content.startsWith("[tool result:") }
            ?: msgs.lastOrNull { it.role == "user" && !it.content.startsWith("[tool result:") }
        val title = firstUser?.content
            ?.lineSequence()?.firstOrNull { it.isNotBlank() }
            ?.take(40)
            ?: "Новый чат"
        val snippet = lastShown?.content
            ?.lineSequence()?.firstOrNull { it.isNotBlank() }
            ?.take(60)
            ?: ""
        val ts = msgs.lastOrNull()?.ts ?: System.currentTimeMillis()
        val updated = _threads.value.map {
            if (it.id == id) it.copy(title = title, lastTs = ts, lastSnippet = snippet) else it
        }.sortedByDescending { it.lastTs }
        _threads.value = updated
        persistIndex(updated)
    }
}
