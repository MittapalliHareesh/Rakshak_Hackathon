package com.shubham.ondevicerag.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.ondevicerag.feature.chat.domain.model.ChatMessage
import com.shubham.ondevicerag.feature.chat.domain.model.ChatRole
import com.shubham.ondevicerag.feature.chat.domain.repository.ChatRepository
import com.shubham.ondevicerag.feature.chat.domain.usecase.DownloadGemmaModelUseCase
import com.shubham.ondevicerag.feature.chat.domain.usecase.PrepareGemmaModelUseCase
import com.shubham.ondevicerag.feature.chat.domain.usecase.ResetChatUseCase
import com.shubham.ondevicerag.feature.chat.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val prepareGemmaModel: PrepareGemmaModelUseCase,
    private val downloadGemmaModel: DownloadGemmaModelUseCase,
    private val sendChatMessage: SendChatMessageUseCase,
    private val resetChat: ResetChatUseCase
) : ViewModel() {
    private var generationJob: Job? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeRuntimeState()
        prepareDownloadedModel()
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value, error = null) }
    }

    fun downloadModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            downloadGemmaModel()
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Model download failed.")
                    }
                }
        }
    }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isBlank() || generationJob?.isActive == true) return

        val currentState = _uiState.value
        if (!currentState.modelStatus.isModelAvailable) {
            _uiState.update { it.copy(error = "Download the Gemma 4 model first.") }
            return
        }

        val priorHistory = currentState.messages.filterNot { it.isStreaming }
        val userMessage = ChatMessage(role = ChatRole.USER, content = text)
        val assistantMessageId = UUID.randomUUID().toString()
        val assistantMessage = ChatMessage(
            id = assistantMessageId,
            role = ChatRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                input = "",
                error = null,
                isGenerating = true,
                messages = it.messages + userMessage + assistantMessage
            )
        }

        generationJob = viewModelScope.launch {
            prepareGemmaModel()
                .onFailure { throwable ->
                    finishAssistantMessage(
                        assistantMessageId,
                        throwable.message ?: "Gemma engine failed to initialize."
                    )
                    return@launch
                }

            var lastResponse = ""
            var failed = false
            sendChatMessage(text, priorHistory)
                .catch { throwable ->
                    failed = true
                    finishAssistantMessage(
                        assistantMessageId,
                        throwable.message ?: "Gemma failed to respond."
                    )
                }
                .collect { partialResponse ->
                    lastResponse = partialResponse
                    updateAssistantMessage(
                        assistantMessageId = assistantMessageId,
                        content = partialResponse,
                        isStreaming = true
                    )
                }

            if (!failed) {
                finishAssistantMessage(
                    assistantMessageId = assistantMessageId,
                    content = lastResponse.ifBlank { "No response." }
                )
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { state ->
            state.copy(
                isGenerating = false,
                messages = state.messages.map { message ->
                    if (message.isStreaming) message.copy(isStreaming = false) else message
                }
            )
        }
    }

    fun newChat() {
        viewModelScope.launch {
            stopGeneration()
            resetChat()
            _uiState.update { it.copy(messages = emptyList(), error = null) }
        }
    }

    private fun observeRuntimeState() {
        viewModelScope.launch {
            chatRepository.modelStatus.collect { modelStatus ->
                _uiState.update { it.copy(modelStatus = modelStatus) }
                if (modelStatus.isModelAvailable && !chatRepository.isEngineReady.value) {
                    prepareDownloadedModel()
                }
            }
        }
        viewModelScope.launch {
            chatRepository.isEngineReady.collect { isReady ->
                _uiState.update { it.copy(isEngineReady = isReady) }
            }
        }
        viewModelScope.launch {
            chatRepository.backendName.collect { backendName ->
                _uiState.update { it.copy(backendName = backendName) }
            }
        }
    }

    private fun prepareDownloadedModel() {
        viewModelScope.launch {
            prepareGemmaModel()
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(error = throwable.message ?: "Gemma engine failed to initialize.")
                    }
                }
        }
    }

    private fun updateAssistantMessage(
        assistantMessageId: String,
        content: String,
        isStreaming: Boolean
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == assistantMessageId) {
                        message.copy(content = content, isStreaming = isStreaming)
                    } else {
                        message
                    }
                }
            )
        }
    }

    private fun finishAssistantMessage(
        assistantMessageId: String,
        content: String
    ) {
        updateAssistantMessage(
            assistantMessageId = assistantMessageId,
            content = content,
            isStreaming = false
        )
        generationJob = null
        _uiState.update { it.copy(isGenerating = false) }
    }
}
