package com.shubham.ondevicerag.feature.chat.presentation

import com.shubham.ondevicerag.feature.chat.domain.model.ChatMessage
import com.shubham.ondevicerag.feature.chat.domain.model.ModelDownloadStatus

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val modelStatus: ModelDownloadStatus = ModelDownloadStatus(),
    val isEngineReady: Boolean = false,
    val backendName: String = "CPU",
    val isGenerating: Boolean = false,
    val error: String? = null
)
