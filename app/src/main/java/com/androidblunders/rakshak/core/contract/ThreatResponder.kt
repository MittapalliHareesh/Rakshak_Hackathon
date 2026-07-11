package com.androidblunders.rakshak.core.contract

import com.androidblunders.rakshak.core.model.ThreatLevel

/**
 * The side-effecting arm of the orchestrator. Given a target [ThreatLevel], a
 * responder performs the actual user-facing intervention: draw / dismiss the
 * overlay, fire the TTS barge-in, vibrate, dispatch the family SMS.
 *
 * Kept behind an interface so the orchestrator's state machine stays pure and
 * unit-testable, and so overlay/alert implementations can be swapped or mocked.
 */
interface ThreatResponder {
    /**
     * React to a confirmed transition into [level]. Implementations must be
     * idempotent for repeat calls with the same level and must not throw.
     */
    suspend fun onThreatLevel(level: ThreatLevel)
}
