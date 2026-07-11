package com.androidblunders.rakshak.gemma

import com.androidblunders.rakshak.core.contract.TextGenerator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.last

/**
 * The reusable offline Gemma 4 endpoint, exposed through the generic
 * [TextGenerator] contract. Any module (spam detector, summarizer, guidance
 * scripter) injects `TextGenerator` and gets on-device inference for free —
 * this class is the only place that knows about LiteRT-LM.
 *
 * Composes [GemmaModelManager] (weights) + [GemmaEngineClient] (inference).
 */
@Singleton
class GemmaTextGenerator @Inject constructor(
    private val modelManager: GemmaModelManager,
    private val engine: GemmaEngineClient,
) : TextGenerator {

    override val isReady: StateFlow<Boolean> = engine.isReady
    override val backend: StateFlow<String> = engine.backendName

    override suspend fun prepare(): Result<Unit> {
        // If weights are already present, just init the engine; otherwise download then init.
        val modelPath = if (modelManager.isModelAvailable()) {
            modelManager.getModelPath()
        } else {
            val downloaded = modelManager.downloadModel().getOrElse { return Result.failure(it) }
            downloaded.absolutePath
        }
        return engine.initialize(modelPath)
    }

    override suspend fun generate(prompt: String, systemInstruction: String?): Result<String> =
        runCatching { engine.generateOnce(prompt, systemInstruction).last() }

    override fun generateStream(prompt: String, systemInstruction: String?): Flow<String> =
        engine.generateOnce(prompt, systemInstruction)
}
