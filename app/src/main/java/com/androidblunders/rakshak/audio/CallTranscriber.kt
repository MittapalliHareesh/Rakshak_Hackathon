package com.androidblunders.rakshak.audio

import android.content.Context
import android.content.Intent
import com.androidblunders.rakshak.services.CallAudioStreamingService

/**
 * Public API for Call Audio Transcription Plugin
 */
object CallTranscriber {

    /**
     * Starts the call audio streaming and transcription service.
     */
    fun startTranscription(context: Context) {
        val intent = Intent(context, CallAudioStreamingService::class.java)
        context.startForegroundService(intent)
    }

    /**
     * Stops the transcription service.
     */
    fun stopTranscription(context: Context) {
        val intent = Intent(context, CallAudioStreamingService::class.java)
        context.stopService(intent)
    }

}
