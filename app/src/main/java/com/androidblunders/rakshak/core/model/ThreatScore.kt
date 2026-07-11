package com.androidblunders.rakshak.core.model

/**
 * Output of a single [com.androidblunders.rakshak.core.contract.ThreatAnalyzer].
 *
 * Every analyzer (Gemma on-device, Gemini Live, a regex rule engine, ...) returns
 * this uniform shape so the fusion engine can combine them without caring which
 * model produced the score.
 */
data class ThreatScore(
    /** Normalized threat confidence in [0.0, 1.0]. 0 = safe, 1 = certain scam. */
    val confidence: Float,
    /** Which analyzer produced this score, e.g. "gemma", "gemini-live". */
    val source: String,
    /** Short human-readable reason, surfaced in the security history / debug view. */
    val rationale: String = "",
    /** True when the analyzer could not run (offline, timeout). Excluded from fusion. */
    val unavailable: Boolean = false,
) {
    companion object {
        fun unavailable(source: String, reason: String = "") =
            ThreatScore(confidence = 0f, source = source, rationale = reason, unavailable = true)

        fun safe(source: String) = ThreatScore(confidence = 0f, source = source)
    }
}
