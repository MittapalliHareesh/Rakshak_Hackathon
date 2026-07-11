package com.androidblunders.rakshak.gemma

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
import com.google.ai.edge.litertlm.SamplerConfig
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

/**
 * Thin wrapper over the LiteRT-LM [Engine]. Owns engine lifecycle, backend
 * selection (GPU with CPU fallback), and stateless one-shot generation.
 *
 * Deliberately generic: no scam/threat concepts here. Each [generateOnce] call
 * spins up a fresh [Conversation] so requests don't share context — exactly what
 * a per-message analyzer wants.
 */
@Singleton
class GemmaEngineClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    // Serializes generateOnce() calls — one engine can't run overlapping conversations.
    private val generationMutex = Mutex()
    private var engine: Engine? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _backendName = MutableStateFlow("CPU")
    val backendName: StateFlow<String> = _backendName.asStateFlow()

    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_isReady.value) return@withLock Result.success(Unit)
            if (!File(modelPath).exists()) {
                return@withLock Result.failure(IllegalStateException("Model file missing: $modelPath"))
            }
            closeLocked()
            enableSpeculativeDecoding()
            initWithBackend(modelPath, Backend.GPU(), "GPU")
                .recoverCatching { initWithBackend(modelPath, Backend.CPU(), "CPU").getOrThrow() }
        }
    }

    /**
     * Stateless completion. Streams cleaned partial text; the collector may take
     * the last emission for a full response, or render intermediate ones for a
     * "typing" effect. Throws if the engine is not initialized.
     */
    fun generateOnce(prompt: String, systemInstruction: String?): Flow<String> = flow {
        // The underlying engine is single-instance; serialize concurrent requests
        // (e.g. the orchestrator and the spam-detection pipeline hitting it for the
        // same message) so conversations don't overlap on one engine.
        generationMutex.withLock {
            val activeEngine = engine ?: throw IllegalStateException("Gemma engine not ready.")
            val conversation = activeEngine.createConversation(configFor(systemInstruction))
            try {
                val acc = StringBuilder()
                conversation.sendMessageAsync(prompt).collect { chunk ->
                    acc.append(chunk.toString())
                    emit(clean(acc.toString()))
                }
            } finally {
                conversation.close()
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun initWithBackend(
        modelPath: String,
        backend: Backend,
        name: String,
    ): Result<Unit> = runCatching {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            visionBackend = Backend.CPU(),
            cacheDir = context.cacheDir.path,
        )
        engine = Engine(config).also { it.initialize() }
        _backendName.value = name
        _isReady.value = true
    }

    private fun configFor(systemInstruction: String?): ConversationConfig =
        ConversationConfig(
            systemInstruction = Contents.of(systemInstruction ?: DEFAULT_SYSTEM),
            channels = listOf(Channel(THINKING_CHANNEL, THINKING_START, THINKING_END)),
            samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.4),
        )

    fun close() {
        // Best-effort synchronous close for process teardown.
        engine?.close()
        engine = null
        _isReady.value = false
    }

    private fun closeLocked() {
        engine?.close()
        engine = null
        _isReady.value = false
    }

    @OptIn(ExperimentalApi::class)
    private fun enableSpeculativeDecoding() {
        ExperimentalFlags.enableSpeculativeDecoding = true
    }

    /** Strip the model's <think>...</think> reasoning channel from the visible output. */
    private fun clean(raw: String): String {
        val start = raw.indexOf(THINKING_START)
        if (start == -1) return raw
        val contentStart = start + THINKING_START.length
        val end = raw.indexOf(THINKING_END, contentStart)
        return if (end == -1) "" else raw.substring(end + THINKING_END.length).trimStart('\n')
    }

    private companion object {
        const val THINKING_CHANNEL = "thinking"
        const val THINKING_START = "<think>"
        const val THINKING_END = "</think>"
        const val DEFAULT_SYSTEM =
            "You are a concise assistant running locally on this Android device."
    }
}
