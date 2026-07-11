package com.shubham.ondevicerag.feature.chat.domain.repository

import com.shubham.ondevicerag.feature.chat.domain.model.ChatMessage
import com.shubham.ondevicerag.feature.chat.domain.model.ModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val modelStatus: StateFlow<ModelDownloadStatus>
    val isEngineReady: StateFlow<Boolean>
    val backendName: StateFlow<String>

    suspend fun prepareModel(): Result<Unit>

    suspend fun downloadModel(): Result<Unit>

    fun streamMessage(
        message: String,
        history: List<ChatMessage>
    ): Flow<String>

    suspend fun resetConversation()
}
