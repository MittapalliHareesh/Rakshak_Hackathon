package com.androidblunders.rakshak.core.contract

import kotlinx.coroutines.flow.Flow

/**
 * Plug-and-play contract for the Call Transcriber's audio capture.
 * Analysis is fully decoupled from capture — this only produces raw PCM.
 *
 * Implementations (arrive later): `RealCallStreamer` (VOICE_COMMUNICATION),
 * `MicMockStreamer` (MIC, for the demo), `FileMockStreamer` (.wav, for tests).
 *
 * Format contract: 16 kHz, mono, 16-bit PCM (required by Gemini Live).
 */
interface AudioStreamer {
    /** Cold flow of PCM chunks. Collecting starts capture; cancelling stops it. */
    fun startStreaming(): Flow<ByteArray>
    fun stopStreaming()
}
