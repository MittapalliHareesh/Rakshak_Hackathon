package com.androidblunders.rakshak.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

class AudioStreamingManager(private val serverUrl: String) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val transcriptionBuilder = StringBuilder()
    private val _transcriptionState = MutableStateFlow("")
    val transcriptionState = _transcriptionState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startStreaming() {
        if (isRecording) return
        
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("AudioStreaming", "WebSocket Opened")
                startRecording()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val transcript = bytes.utf8()
                synchronized(transcriptionBuilder) {
                    transcriptionBuilder.append(transcript).append(" ")
                    _transcriptionState.value = transcriptionBuilder.toString()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                synchronized(transcriptionBuilder) {
                    transcriptionBuilder.append(text).append(" ")
                    _transcriptionState.value = transcriptionBuilder.toString()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("AudioStreaming", "WebSocket Failure: ${t.message}")
                stopStreaming()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.d("AudioStreaming", "WebSocket Closing: $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, // Optimized for call/voice
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioStreaming", "AudioRecord initialization failed")
            return
        }

        audioRecord?.startRecording()
        isRecording = true

        scope.launch {
            val audioBuffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (read > 0) {
                    val data = audioBuffer.copyOfRange(0, read)
                    webSocket?.send(data.toByteString())
                }
            }
        }
    }

    fun stopStreaming() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket?.close(1000, "Stopped by user")
        webSocket = null
    }

    fun getFullTranscription(): String {
        return synchronized(transcriptionBuilder) {
            transcriptionBuilder.toString()
        }
    }

    fun clearTranscription() {
        synchronized(transcriptionBuilder) {
            transcriptionBuilder.setLength(0)
            _transcriptionState.value = ""
        }
    }
}
