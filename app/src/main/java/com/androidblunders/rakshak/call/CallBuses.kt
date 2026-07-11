package com.androidblunders.rakshak.call

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Decoupling bus for the live-call pipeline:
 *
 *   CallAudioStreamingService ──PCM bytes──► VOICE (ws) ──transcript──►
 *       AudioStreamingManager ──► [LiveTranscriptBus] ──► SpamDetectionOrchestrator
 *
 * Anything that produces live speech text (the VOICE socket, a local STT engine,
 * or a demo button) just calls [LiveTranscriptBus.push]; the spam-detection
 * pipeline consumes it exactly like an inbound message.
 */

/** A finalized transcript turn from the STT / VOICE backend. */
data class LiveTranscript(
    val text: String,
    val speaker: String = "Caller",
    val timestamp: Long = System.currentTimeMillis(),
)

object LiveTranscriptBus {
    private val _transcripts = MutableSharedFlow<LiveTranscript>(extraBufferCapacity = 16)
    val transcripts: SharedFlow<LiveTranscript> = _transcripts.asSharedFlow()

    fun push(text: String, speaker: String = "Caller") {
        if (text.isBlank()) return
        _transcripts.tryEmit(LiveTranscript(text = text, speaker = speaker))
    }
}
