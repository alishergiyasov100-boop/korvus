package com.musornibak.korvus.data.store

import android.content.Context
import com.musornibak.korvus.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class MessageStore(ctx: Context) {

    private val file: File = File(ctx.filesDir, "messages.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Message.serializer())
    private val _messages = MutableStateFlow<List<Message>>(load())
    val messages = _messages.asStateFlow()

    private fun load(): List<Message> = try {
        if (file.exists()) json.decodeFromString(serializer, file.readText()) else emptyList()
    } catch (_: Throwable) {
        emptyList()
    }

    suspend fun append(m: Message) = withContext(Dispatchers.IO) {
        val next = _messages.value + m
        _messages.value = next
        persist(next)
    }

    suspend fun replaceLast(m: Message) = withContext(Dispatchers.IO) {
        val cur = _messages.value.toMutableList()
        if (cur.isNotEmpty()) cur[cur.size - 1] = m
        _messages.value = cur
        persist(cur)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        _messages.value = emptyList()
        if (file.exists()) file.delete()
    }

    private fun persist(list: List<Message>) {
        try { file.writeText(json.encodeToString(serializer, list)) } catch (_: Throwable) {}
    }
}
