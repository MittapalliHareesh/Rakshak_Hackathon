package com.androidblunders.rakshak.core.model

/**
 * User-tunable protection settings, held by the
 * [com.androidblunders.rakshak.orchestrator.SessionManager] and injected wherever needed.
 */
data class UserPreferences(
    val protectionEnabled: Boolean = true,
    /** Spoken cheat-code that flips the app into GENTLE_GUIDANCE mode. */
    val guidanceCheatCode: String = "beta help",
    /** Family contacts that receive the emergency SMS on [ThreatLevel.EMERGENCY]. */
    val emergencyContacts: List<String> = emptyList(),
    /** Preferred TTS language tag, e.g. "hi-IN" or "en-IN". */
    val voiceLanguageTag: String = "en-IN",
    val attachLocationToAlerts: Boolean = true,
)
