package com.androidblunders.rakshak.call

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Live call / mic recording foreground service.
 *
 * Captures 16 kHz mono 16-bit PCM (the format Gemini Live and most STT engines
 * expect) and publishes each buffer to [AudioCaptureBus]. It does NOT transcribe
 * — the STT module (built separately) consumes [AudioCaptureBus] and pushes text
 * to [LiveTranscriptBus]. Running as a foreground service keeps mic access alive
 * while the app is backgrounded during a call.
 *
 * Fault tolerance: if another app grabs exclusive mic control, capture restarts
 * every [RESTART_DELAY_MS] rather than crashing.
 *
 * Control: `CallRecordingService.start(context)` / `.stop(context)`.
 */
class CallRecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        startAsForeground()
        if (captureJob != null) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO not granted — stopping service.")
            stopSelf()
            return
        }

        captureJob = scope.launch {
            while (isActive) {
                try {
                    recordLoop()
                } catch (e: Exception) {
                    ensureActive()
                    Log.w(TAG, "Capture failed; retrying in ${RESTART_DELAY_MS}ms", e)
                    kotlinx.coroutines.delay(RESTART_DELAY_MS)
                }
            }
        }
    }

    private suspend fun recordLoop() {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
            .coerceAtLeast(BUFFER_BYTES)

        @Suppress("MissingPermission")
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE, CHANNEL, ENCODING, minBuffer,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord failed to initialize")
        }

        val buffer = ByteArray(BUFFER_BYTES)
        try {
            recorder.startRecording()
            Log.i(TAG, "Recording started @ ${SAMPLE_RATE}Hz mono PCM16")
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    AudioCaptureBus.emit(buffer.copyOf(read))
                }
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Call Protection", NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Rakshak is monitoring your call for scams." }
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rakshak is protecting you")
            .setContentText("Listening for scams during your call.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        captureJob?.cancel()
        captureJob = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallRecordingService"
        private const val CHANNEL_ID = "rakshak_call_protection"
        private const val NOTIFICATION_ID = 4201
        private const val ACTION_STOP = "com.androidblunders.rakshak.STOP_RECORDING"

        // Strict format required by Gemini Live / STT: 16 kHz mono 16-bit PCM.
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_BYTES = 3_200 // ~100ms at 16kHz mono PCM16
        private const val RESTART_DELAY_MS = 5_000L

        fun start(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
