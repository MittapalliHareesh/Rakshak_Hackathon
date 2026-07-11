package com.shubham.ondevicerag.feature.chat.data.repository

import com.shubham.ondevicerag.feature.chat.data.ai.GemmaEngineClient
import com.shubham.ondevicerag.feature.chat.data.ai.GemmaModelManager
import com.shubham.ondevicerag.feature.chat.domain.model.ChatMessage
import com.shubham.ondevicerag.feature.chat.domain.model.ModelDownloadStatus
import com.shubham.ondevicerag.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class GemmaChatRepository @Inject constructor(
    private val modelManager: GemmaModelManager,
    private val engineClient: GemmaEngineClient
) : ChatRepository {
    override val modelStatus: StateFlow<ModelDownloadStatus> = modelManager.modelStatus
    override val isEngineReady: StateFlow<Boolean> = engineClient.isReady
    override val backendName: StateFlow<String> = engineClient.backendName

    override suspend fun prepareModel(): Result<Unit> {
        if (!modelManager.isModelAvailable()) return Result.success(Unit)
        return engineClient.initialize(modelManager.getModelPath())
    }

    override suspend fun downloadModel(): Result<Unit> {
        return modelManager.downloadModel().mapCatching { modelFile ->
            engineClient.initialize(modelFile.absolutePath).getOrThrow()
        }
    }

    override fun streamMessage(
        message: String,
        history: List<ChatMessage>
    ): Flow<String> = engineClient.streamMessage(message, history)

    override suspend fun resetConversation() {
        engineClient.resetConversation()
    }
}
