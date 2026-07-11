package com.shubham.ondevicerag.feature.chat.domain.usecase

import com.shubham.ondevicerag.feature.chat.domain.repository.ChatRepository
import javax.inject.Inject

class ResetChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke() {
        repository.resetConversation()
    }
}
