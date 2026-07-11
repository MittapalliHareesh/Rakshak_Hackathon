package com.androidblunders.rakshak.core.model

/**
 * The unified analysis input handed to every [com.androidblunders.rakshak.core.contract.ThreatAnalyzer].
 *
 * It deliberately does NOT know whether the text came from an SMS, a live call
 * transcript, a WhatsApp notification, or a test mock — the source modules
 * normalize everything into this shape. This is what makes analyzers
 * plug-and-play across channels.
 */
data class CallContext(
    /** Correlates all turns/messages belonging to one call or SMS thread. */
    val sessionId: String,
    /** Where this content came from. */
    val channel: Channel,
    /** The party on the other side (phone number, sender id, app name). */
    val sender: String = "",
    /**
     * Rolling window of the most recent conversational turns (oldest first).
     * For SMS this is usually a single entry; for a live call the STT/transcriber
     * keeps appending. Analyzers should read [latestUtterance] for the newest text.
     */
    val turns: List<Turn> = emptyList(),
) {
    val latestUtterance: String
        get() = turns.lastOrNull()?.text.orEmpty()

    val fullTranscript: String
        get() = turns.joinToString("\n") { "${it.speaker}: ${it.text}" }

    enum class Channel { SMS, VOICE_CALL, NOTIFICATION, UNKNOWN }

    enum class Speaker { REMOTE, USER, SYSTEM }

    data class Turn(
        val speaker: Speaker,
        val text: String,
        val timestamp: Long = System.currentTimeMillis(),
    )
}
