package com.shubham.ondevicerag.feature.chat.domain.usecase

import com.shubham.ondevicerag.feature.chat.domain.model.ChatMessage
import com.shubham.ondevicerag.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(
        message: String,
        history: List<ChatMessage>
    ): Flow<String> = repository.streamMessage(message, history)
}
