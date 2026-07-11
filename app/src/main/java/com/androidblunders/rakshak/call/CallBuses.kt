package com.androidblunders.rakshak.call

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Decoupling buses for the live-call pipeline. Mirrors the [com.androidblunders.rakshak.messaging.MessageExtractor]
 * pattern so the separate STT team has a dead-simple contract:
 *
 *   [CallRecordingService] ──PCM──► [AudioCaptureBus] ──► (STT module) ──text──► [LiveTranscriptBus] ──► SpamDetection
 *
 * Neither the recorder nor the spam detector needs to know how STT is implemented.
 */

/** Raw 16 kHz mono 16-bit PCM chunks emitted by the recording service. */
object AudioCaptureBus {
    private val _pcm = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
    val pcm: SharedFlow<ByteArray> = _pcm.asSharedFlow()

    /** Called by the recording service for each captured buffer. */
    fun emit(chunk: ByteArray) {
        _pcm.tryEmit(chunk)
    }
}

/** A finalized transcript turn from the (separately-built) STT engine. */
data class LiveTranscript(
    val text: String,
    val speaker: String = "Caller",
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Live speech-to-text output. The STT module pushes recognized text here; the
 * spam-detection pipeline consumes it exactly like an inbound message.
 *
 * This is the "input as live text" hand-off point the STT team plugs into —
 * `LiveTranscriptBus.push("This is the CBI, a warrant is issued...")`.
 */
object LiveTranscriptBus {
    private val _transcripts = MutableSharedFlow<LiveTranscript>(extraBufferCapacity = 16)
    val transcripts: SharedFlow<LiveTranscript> = _transcripts.asSharedFlow()

    fun push(text: String, speaker: String = "Caller") {
        if (text.isBlank()) return
        _transcripts.tryEmit(LiveTranscript(text = text, speaker = speaker))
    }
}
