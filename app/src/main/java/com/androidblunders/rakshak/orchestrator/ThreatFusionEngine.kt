package com.androidblunders.rakshak.orchestrator

import com.androidblunders.rakshak.core.model.CallContext
import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.core.model.ThreatScore
import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth for the app's protective state.
 *
 * It runs every bound [com.androidblunders.rakshak.core.contract.ThreatAnalyzer]
 * over a [CallContext], fuses their scores with graceful degradation (drop
 * unavailable analyzers and renormalize weights), and exposes the resulting
 * [ThreatLevel] as a [StateFlow] the whole UI observes.
 */
interface ThreatFusionEngine {

    /** Observed by overlays, activities and the orchestrator. */
    val currentThreatLevel: StateFlow<ThreatLevel>

    /** The most recent fused confidence in [0,1], for the debug/history views. */
    val currentConfidence: StateFlow<Float>

    /** The individual per-analyzer scores from the last run, for diagnostics. */
    val lastScores: StateFlow<List<ThreatScore>>

    /** Analyze [context] with all analyzers and publish the fused level. */
    suspend fun evaluate(context: CallContext): ThreatLevel

    /** Force a specific level (e.g. cheat-code -> GENTLE_GUIDANCE, or reset to IDLE). */
    fun override(level: ThreatLevel)

    /**
     * Publish a score computed by a specialised pipeline that owns its own
     * analyzers, while keeping the dashboard and responders on this shared state.
     */
    fun publish(level: ThreatLevel, confidence: Float)

    fun reset()
}
