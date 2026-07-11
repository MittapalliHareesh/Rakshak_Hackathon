package com.androidblunders.rakshak.orchestrator

import android.util.Log
import com.androidblunders.rakshak.core.contract.ThreatAnalyzer
import com.androidblunders.rakshak.core.model.CallContext
import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.core.model.ThreatScore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Weighted-fusion implementation.
 *
 * Fusion is `sum(confidence_i * weight_i) / sum(weight_i)` over only the
 * *available* analyzers. If Gemini Live drops out, its score is marked
 * unavailable, excluded, and the weights renormalize — so the formula naturally
 * collapses to `Gemma * 1.0` with no branching in the orchestrator.
 */
@Singleton
class DefaultThreatFusionEngine @Inject constructor(
    private val analyzers: Set<@JvmSuppressWildcards ThreatAnalyzer>,
) : ThreatFusionEngine {

    private val _currentThreatLevel = MutableStateFlow(ThreatLevel.IDLE)
    override val currentThreatLevel: StateFlow<ThreatLevel> = _currentThreatLevel.asStateFlow()

    private val _currentConfidence = MutableStateFlow(0f)
    override val currentConfidence: StateFlow<Float> = _currentConfidence.asStateFlow()

    private val _lastScores = MutableStateFlow<List<ThreatScore>>(emptyList())
    override val lastScores: StateFlow<List<ThreatScore>> = _lastScores.asStateFlow()

    override suspend fun evaluate(context: CallContext): ThreatLevel = coroutineScope {
        // A GENTLE_GUIDANCE override (cheat code) must not be clobbered by scoring.
        if (_currentThreatLevel.value == ThreatLevel.GENTLE_GUIDANCE) {
            return@coroutineScope ThreatLevel.GENTLE_GUIDANCE
        }

        val scores = analyzers
            .map { analyzer -> async { runAnalyzer(analyzer, context) } }
            .map { it.await() }
        _lastScores.value = scores

        val available = scores.filterNot { it.unavailable }
        val fused = fuse(available)
        _currentConfidence.value = fused

        val level = levelFor(fused)
        _currentThreatLevel.value = level
        level
    }

    private suspend fun runAnalyzer(analyzer: ThreatAnalyzer, context: CallContext): ThreatScore =
        try {
            analyzer.analyze(context)
        } catch (t: Throwable) {
            Log.w(TAG, "Analyzer '${analyzer.id}' failed, degrading gracefully.", t)
            ThreatScore.unavailable(analyzer.id, t.message ?: "error")
        }

    private fun fuse(available: List<ThreatScore>): Float {
        if (available.isEmpty()) return 0f
        // Map score.source -> weight via the analyzer set.
        val weightBySource = analyzers.associate { it.id to it.weight.coerceAtLeast(0f) }
        val totalWeight = available.sumOf { (weightBySource[it.source] ?: 1f).toDouble() }
        if (totalWeight <= 0.0) return available.map { it.confidence }.average().toFloat()
        val weighted = available.sumOf {
            (it.confidence * (weightBySource[it.source] ?: 1f)).toDouble()
        }
        return (weighted / totalWeight).toFloat().coerceIn(0f, 1f)
    }

    private fun levelFor(confidence: Float): ThreatLevel = when {
        confidence >= EMERGENCY_THRESHOLD -> ThreatLevel.EMERGENCY
        confidence >= ACTIVE_THRESHOLD -> ThreatLevel.ACTIVE_THREAT
        confidence >= MEDIUM_THRESHOLD -> ThreatLevel.MEDIUM
        confidence >= LOW_THRESHOLD -> ThreatLevel.LOW
        else -> ThreatLevel.IDLE
    }

    override fun override(level: ThreatLevel) {
        _currentThreatLevel.value = level
    }

    override fun reset() {
        _currentThreatLevel.value = ThreatLevel.IDLE
        _currentConfidence.value = 0f
        _lastScores.value = emptyList()
    }

    private companion object {
        const val TAG = "FusionEngine"
        const val LOW_THRESHOLD = 0.30f
        const val MEDIUM_THRESHOLD = 0.55f
        const val ACTIVE_THRESHOLD = 0.75f
        const val EMERGENCY_THRESHOLD = 0.92f
    }
}
