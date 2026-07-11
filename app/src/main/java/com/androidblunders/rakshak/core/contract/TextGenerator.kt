package com.androidblunders.rakshak.core.contract

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Offline, on-device text generation — the reusable Gemma 4 endpoint.
 *
 * This is intentionally generic: it knows nothing about scams, threats, or the
 * orchestrator. ANY module that wants offline LLM support (spam detector,
 * summarizer, guidance-script generator, ...) injects this single interface and
 * calls [generate] / [generateStream]. The concrete implementation
 * (`GemmaTextGenerator`) can be swapped for a mock in tests without touching callers.
 */
interface TextGenerator {

    /** True once the model is loaded into the inference engine and can serve requests. */
    val isReady: StateFlow<Boolean>

    /** Human-readable backend actually in use ("GPU" / "CPU"), for diagnostics. */
    val backend: StateFlow<String>

    /**
     * Ensure the model is downloaded (if needed) and the engine is initialized.
     * Idempotent and safe to call repeatedly. Returns failure with a cause on error.
     */
    suspend fun prepare(): Result<Unit>

    /**
     * One-shot, stateless completion. Each call is independent (fresh context),
     * which is what analyzers want — no cross-request memory bleed.
     *
     * @param prompt the user/content turn.
     * @param systemInstruction optional persona/task framing.
     */
    suspend fun generate(prompt: String, systemInstruction: String? = null): Result<String>

    /**
     * Streaming variant: emits progressively cleaned partial text as tokens arrive.
     * Useful for UI that shows the model "typing".
     */
    fun generateStream(prompt: String, systemInstruction: String? = null): Flow<String>
}
