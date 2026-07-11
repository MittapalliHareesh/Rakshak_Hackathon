package com.shubham.ondevicerag.feature.chat.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val isStreaming: Boolean = false
)

enum class ChatRole {
    USER,
    ASSISTANT
}
