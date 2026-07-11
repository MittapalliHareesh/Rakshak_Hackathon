package com.androidblunders.rakshak.core.model

/**
 * The single source of truth for the whole app's protective state.
 *
 * The [com.androidblunders.rakshak.orchestrator.ThreatFusionEngine] emits this,
 * every UI surface (overlays, dashboard) observes it, and the
 * [com.androidblunders.rakshak.orchestrator.RakshakOrchestrator] reacts to
 * transitions by driving TTS / overlays / family alerts.
 */
enum class ThreatLevel {
    /** Background monitoring, nothing suspicious. Dashboard shows "Monitoring Active". */
    IDLE,

    /** Faint signal. Logged to security history only, no user-facing interruption. */
    LOW,

    /** Suspicious. Subtle UI warning + vibration, but not a full takeover. */
    MEDIUM,

    /** Confirmed digital-arrest / extortion scam. Red overlay + aggressive TTS barge-in. */
    ACTIVE_THREAT,

    /** User triggered the cheat-code ("Beta, help"). Calming green overlay + soft TTS. */
    GENTLE_GUIDANCE,

    /** Escalation past ACTIVE_THREAT. Dispatch family SMS alert with location. */
    EMERGENCY;

    val isIntervention: Boolean
        get() = this == ACTIVE_THREAT || this == GENTLE_GUIDANCE || this == EMERGENCY
}
