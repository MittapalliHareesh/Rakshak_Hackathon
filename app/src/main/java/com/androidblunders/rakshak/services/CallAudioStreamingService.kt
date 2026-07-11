package com.androidblunders.rakshak.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.androidblunders.rakshak.MainActivity
import com.androidblunders.rakshak.R
import com.androidblunders.rakshak.BuildConfig
import com.androidblunders.rakshak.audio.AudioStreamingManager
import com.androidblunders.rakshak.call.CallStreamStatus

class CallAudioStreamingService : Service() {

    private var streamingManager: AudioStreamingManager? = null

    companion object {
        private const val CHANNEL_ID = "CallAudioStreamingChannel"
        private const val NOTIFICATION_ID = 1002
        private var instance: CallAudioStreamingService? = null
        
        fun getTranscription(): String = instance?.streamingManager?.getFullTranscription() ?: ""
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CallStreamStatus.reset()
        val callId = java.util.UUID.randomUUID().toString()
        val baseUrl = BuildConfig.VOICE_WS_BASE.trimEnd('/')
        streamingManager = AudioStreamingManager("$baseUrl/ws/call/$callId")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        CallStreamStatus.setActive(true)
        streamingManager?.startStreaming()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        streamingManager?.close()
        CallStreamStatus.setActive(false)
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Call Audio Streaming Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
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
