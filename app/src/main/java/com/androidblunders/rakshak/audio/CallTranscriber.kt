package com.androidblunders.rakshak.audio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.androidblunders.rakshak.services.CallAudioStreamingService

/**
 * Public API for Call Audio Transcription Plugin
 */
object CallTranscriber {

    /**
     * Starts the call audio streaming and transcription service.
     */
    fun startTranscription(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Microphone permission is not granted; streaming service not started.")
            return
        }
        val intent = Intent(context, CallAudioStreamingService::class.java)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { Log.e(TAG, "Unable to start call streaming service", it) }
    }

    /**
     * Stops the transcription service.
     */
    fun stopTranscription(context: Context) {
        val intent = Intent(context, CallAudioStreamingService::class.java)
        context.stopService(intent)
    }

    private const val TAG = "CallTranscriber"

}
