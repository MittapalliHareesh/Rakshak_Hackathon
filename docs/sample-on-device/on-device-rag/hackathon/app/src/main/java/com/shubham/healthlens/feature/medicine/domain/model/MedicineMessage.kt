package com.shubham.healthlens.feature.medicine.domain.model

import java.util.UUID

data class MedicineMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val isStreaming: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT
}
