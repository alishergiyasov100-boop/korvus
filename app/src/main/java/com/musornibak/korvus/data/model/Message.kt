package com.musornibak.korvus.data.model

import kotlinx.serialization.Serializable

enum class Role { USER, ASSISTANT, SYSTEM, TOOL }

@Serializable
data class Message(
    val role: String,
    val content: String,
    val ts: Long = System.currentTimeMillis(),
    val modelId: String? = null
)
