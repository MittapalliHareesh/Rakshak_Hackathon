package com.androidblunders.rakshak.core.contract

import com.androidblunders.rakshak.core.model.CallContext
import com.androidblunders.rakshak.core.model.ThreatScore

/**
 * Plug-and-play contract for anything that can judge whether a conversation is a scam.
 *
 * Implementations: `GemmaThreatAnalyzer` (offline), a future `GeminiLiveThreatAnalyzer`
 * (online), a `RegexThreatAnalyzer`, etc. They are collected into a `Set` by Hilt and
 * combined by the [com.androidblunders.rakshak.orchestrator.ThreatFusionEngine].
 *
 * Adding a new model = implement this + bind it @IntoSet. No orchestrator change.
 */
interface ThreatAnalyzer {
    /** Stable id used for fusion weighting and diagnostics, e.g. "gemma". */
    val id: String

    /** Relative weight in the fusion formula. Weights are renormalized over available analyzers. */
    val weight: Float

    /**
     * Analyze the current context. Must NOT throw — return
     * [ThreatScore.unavailable] on failure so fusion can degrade gracefully.
     */
    suspend fun analyze(context: CallContext): ThreatScore
}
