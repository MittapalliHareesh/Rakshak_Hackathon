package com.shubham.ondevicerag.feature.chat.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Channel
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.shubham.ondevicerag.feature.chat.domain.model.ChatMessage
import com.shubham.ondevicerag.feature.chat.domain.model.ChatRole
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class GemmaEngineClient @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _backendName = MutableStateFlow("CPU")
    val backendName: StateFlow<String> = _backendName.asStateFlow()

    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_isReady.value) return@withLock Result.success(Unit)

            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                return@withLock Result.failure(IllegalStateException("Model file does not exist."))
            }

            closeLocked()
            enableSpeculativeDecoding()

            initializeWithBackend(modelPath, Backend.GPU(), "GPU")
                .recoverCatching {
                    initializeWithBackend(modelPath, Backend.CPU(), "CPU").getOrThrow()
                }
        }
    }

    fun streamMessage(
        message: String,
        history: List<ChatMessage>
    ): Flow<String> = flow {
        val activeConversation = mutex.withLock {
            val activeEngine = engine ?: throw IllegalStateException("Gemma engine is not ready.")
            conversation ?: activeEngine.createConversation(buildConversationConfig(history))
                .also { conversation = it }
        }

        val accumulatedResponse = StringBuilder()
        activeConversation.sendMessageAsync(message)
            .collect { chunk ->
                accumulatedResponse.append(chunk.toString())
                emit(cleanResponse(accumulatedResponse.toString()))
            }
    }.flowOn(Dispatchers.IO)

    suspend fun resetConversation() {
        mutex.withLock {
            conversation?.close()
            conversation = null
        }
    }

    private fun initializeWithBackend(
        modelPath: String,
        backend: Backend,
        backendName: String
    ): Result<Unit> = runCatching {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            visionBackend = Backend.CPU(),
            cacheDir = context.cacheDir.path
        )
        engine = Engine(config).also { it.initialize() }
        _backendName.value = backendName
        _isReady.value = true
    }

    private fun buildConversationConfig(history: List<ChatMessage>): ConversationConfig {
        val initialMessages = history
            .takeLast(RECENT_HISTORY_LIMIT)
            .mapNotNull { message ->
                when (message.role) {
                    ChatRole.USER -> Message.user(message.content)
                    ChatRole.ASSISTANT -> Message.model(message.content)
                }
            }

        return if (initialMessages.isEmpty()) {
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_PROMPT),
                channels = listOf(Channel(THINKING_CHANNEL, THINKING_START, THINKING_END)),
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.8)
            )
        } else {
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_PROMPT),
                initialMessages = initialMessages,
                channels = listOf(Channel(THINKING_CHANNEL, THINKING_START, THINKING_END)),
                samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.8)
            )
        }
    }

    private fun closeLocked() {
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
        _isReady.value = false
    }

    @OptIn(ExperimentalApi::class)
    private fun enableSpeculativeDecoding() {
        ExperimentalFlags.enableSpeculativeDecoding = true
    }

    private fun cleanResponse(rawText: String): String {
        val startIndex = rawText.indexOf(THINKING_START)
        if (startIndex == -1) return rawText

        val contentStart = startIndex + THINKING_START.length
        val endIndex = rawText.indexOf(THINKING_END, contentStart)
        if (endIndex == -1) return ""

        return rawText.substring(endIndex + THINKING_END.length).trimStart('\n')
    }

    companion object {
        private const val THINKING_CHANNEL = "thinking"
        private const val THINKING_START = "<think>"
        private const val THINKING_END = "</think>"
        private const val RECENT_HISTORY_LIMIT = 20
        private const val SYSTEM_PROMPT =
            "You are a concise, helpful assistant running locally on this Android device."
    }
}
