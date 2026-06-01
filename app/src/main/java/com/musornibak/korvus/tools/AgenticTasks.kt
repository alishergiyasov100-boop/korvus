package com.musornibak.korvus.tools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AgenticTask(
    val id: String,
    val title: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)

object AgenticTasks {
    private val _tasks = MutableStateFlow<List<AgenticTask>>(emptyList())
    val tasks: StateFlow<List<AgenticTask>> = _tasks.asStateFlow()

    private var seq = 0

    @Synchronized
    fun create(title: String, status: String = "pending"): AgenticTask {
        seq += 1
        val t = AgenticTask(id = "t${seq}", title = title, status = status)
        _tasks.value = _tasks.value + t
        return t
    }

    @Synchronized
    fun update(id: String, status: String? = null, title: String? = null): AgenticTask? {
        val cur = _tasks.value.toMutableList()
        val idx = cur.indexOfFirst { it.id == id }
        if (idx < 0) return null
        val old = cur[idx]
        val nw = old.copy(
            status = status ?: old.status,
            title = title ?: old.title
        )
        cur[idx] = nw
        _tasks.value = cur
        return nw
    }

    @Synchronized
    fun remove(id: String): Boolean {
        val cur = _tasks.value
        val next = cur.filter { it.id != id }
        if (next.size == cur.size) return false
        _tasks.value = next
        return true
    }

    fun clear() {
        _tasks.value = emptyList()
    }
}
