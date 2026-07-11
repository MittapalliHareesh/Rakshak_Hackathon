package com.androidblunders.rakshak.generator

import com.androidblunders.rakshak.core.contract.TextGenerator
import com.androidblunders.rakshak.gemma.GemmaModelManager
import com.androidblunders.rakshak.gemma.GemmaTextGenerator
import com.androidblunders.rakshak.gemini.GeminiApiTextGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A wrapper TextGenerator that routes traffic based on offline model availability.
 * If Gemma 4 is downloaded and available, it uses the local engine.
 * If not, it falls back to the cloud Gemini API.
 */
@Singleton
class FallbackTextGenerator @Inject constructor(
    private val modelManager: GemmaModelManager,
    private val gemmaGenerator: GemmaTextGenerator,
    private val geminiGenerator: GeminiApiTextGenerator
) : TextGenerator {

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _backend = MutableStateFlow("Initializing...")
    override val backend: StateFlow<String> = _backend.asStateFlow()

    override suspend fun prepare(): Result<Unit> {
        if (modelManager.isModelAvailable()) {
            _backend.value = "Local (Gemma 4 LiteRT)"
            val localResult = gemmaGenerator.prepare()
            if (localResult.isSuccess) {
                _isReady.value = true
                return localResult
            }
        }

        _backend.value = "Cloud (Gemini 2.5 Flash API)"
        val cloudResult = geminiGenerator.prepare()
        _isReady.value = cloudResult.isSuccess
        return cloudResult
    }

    override suspend fun generate(prompt: String, systemInstruction: String?): Result<String> {
        if (gemmaGenerator.isReady.value) {
            val localResult = gemmaGenerator.generate(prompt, systemInstruction)
            if (localResult.isSuccess) return localResult
        }

        _backend.value = "Cloud (Gemini 2.5 Flash API)"
        val ready = geminiGenerator.prepare()
        if (ready.isFailure) return Result.failure(ready.exceptionOrNull()!!)
        _isReady.value = true
        return geminiGenerator.generate(prompt, systemInstruction)
    }

    override fun generateStream(prompt: String, systemInstruction: String?): Flow<String> {
        return if (gemmaGenerator.isReady.value) {
            gemmaGenerator.generateStream(prompt, systemInstruction)
        } else {
            geminiGenerator.generateStream(prompt, systemInstruction)
        }
    }
}
