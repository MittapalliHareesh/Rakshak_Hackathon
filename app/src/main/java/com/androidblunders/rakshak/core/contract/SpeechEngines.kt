package com.androidblunders.rakshak.core.contract

import com.androidblunders.rakshak.core.model.Priority
import kotlinx.coroutines.flow.SharedFlow

/**
 * Plug-and-play STT contract. Emits recognized text for the local analyzers.
 * Implementations (later): `GeminiLiveStt` (online), `AndroidSpeechRecognizerStt` (offline fallback).
 */
interface SpeechToTextEngine {
    /** Hot stream of finalized transcription fragments. */
    val transcriptions: SharedFlow<String>
    fun start()
    fun stop()
}

/**
 * Plug-and-play TTS contract used by the orchestrator to speak interventions.
 * Implementations (later): `GeminiLiveTts` (primary), `AndroidNativeTts` (offline fallback).
 * Delivery of a [Priority.CRITICAL] barge-in is guaranteed via the fallback chain.
 */
interface TextToSpeechEngine {
    suspend fun speak(text: String, priority: Priority)
    fun stop()
}
