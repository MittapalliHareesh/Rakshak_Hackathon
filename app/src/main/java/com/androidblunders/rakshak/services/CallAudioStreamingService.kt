package com.androidblunders.rakshak.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.androidblunders.rakshak.MainActivity
import com.androidblunders.rakshak.R
import com.androidblunders.rakshak.BuildConfig
import com.androidblunders.rakshak.audio.AudioStreamingManager
import com.androidblunders.rakshak.call.CallStreamStatus
import com.androidblunders.rakshak.core.status.ProtectionRuntimeStatus

class CallAudioStreamingService : Service() {

    private var streamingManager: AudioStreamingManager? = null

    companion object {
        private const val CHANNEL_ID = "CallAudioStreamingChannel"
        private const val NOTIFICATION_ID = 1002
        private const val TAG = "CallAudioService"
    }

    override fun onCreate() {
        super.onCreate()
        CallStreamStatus.reset()
        val callId = java.util.UUID.randomUUID().toString()
        val baseUrl = BuildConfig.VOICE_WS_BASE.trimEnd('/')
        streamingManager = AudioStreamingManager("$baseUrl/ws/call/$callId")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Microphone permission is not granted; stopping call streaming service.")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = createNotification()
        try {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } catch (error: SecurityException) {
            // Android rejects microphone foreground services when the app is not
            // eligible to use the while-in-use microphone permission. Do not
            // let a phone-state broadcast crash the process in that case.
            Log.w(TAG, "Microphone foreground service launch was rejected", error)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        CallStreamStatus.setActive(true)
        ProtectionRuntimeStatus.markCallActivated()
        streamingManager?.startStreaming()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        streamingManager?.close()
        CallStreamStatus.setActive(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Call Audio Streaming Service",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Analysis Active")
            .setContentText("Streaming call audio for real-time transcription...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()
    }
}
