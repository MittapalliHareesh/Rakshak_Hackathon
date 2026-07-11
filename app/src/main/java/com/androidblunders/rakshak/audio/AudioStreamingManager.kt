package com.androidblunders.rakshak.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.androidblunders.rakshak.call.CallStreamStatus
import com.androidblunders.rakshak.call.LiveTranscriptBus
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

/**
 * Streams live 16 kHz mono PCM to the VOICE WebSocket backend
 * (`/ws/call/{call_id}`) and consumes its JSON responses.
 *
 * VOICE protocol:
 *  - Client → server: **binary** PCM chunks; a `{"command":"stop"}` text frame ends it.
 *  - Server → client: **JSON** frames — `transcript_update` / `state_changed`
 *    (with `transcript_chunk`, `threat_score`, `state`, and a base64 `warning_audio`).
 *
 * Wiring:
 *  - Each `transcript_chunk` is pushed to [LiveTranscriptBus] so the on-device
 *    spam-detection pipeline (Gemma/Gemini) analyzes it and drives ThreatLevel.
 *  - Any `warning_audio` is decoded to PCM bytes and played back via [AudioTrack].
 */
class AudioStreamingManager(private val serverUrl: String) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    @Volatile private var isRecording = false
    private val started = AtomicBoolean(false)

    private val transcriptionBuilder = StringBuilder()
    private val _transcriptionState = MutableStateFlow("")
    val transcriptionState = _transcriptionState.asStateFlow()

    private val _threatScore = MutableStateFlow(0f)
    val threatScore = _threatScore.asStateFlow()

    private val _callState = MutableStateFlow("MONITORING")
    val callState = _callState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startStreaming() {
        if (!started.compareAndSet(false, true)) return
        CallStreamStatus.setError(null)
        CallStreamStatus.setConnection(CallStreamStatus.Connection.CONNECTING)
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!started.get()) {
                    webSocket.close(1000, "Cancelled")
                    return
                }
                Log.d(TAG, "WebSocket open → $serverUrl")
                CallStreamStatus.setConnection(CallStreamStatus.Connection.CONNECTED)
                runCatching { startRecording() }
                    .onFailure { error ->
                        Log.e(TAG, "Audio capture failed", error)
                        CallStreamStatus.setError(error.message ?: "Audio capture failed")
                        cleanup(CallStreamStatus.Connection.ERROR)
                    }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                CallStreamStatus.addReceived(text.toByteArray().size)
                handleServerJson(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                CallStreamStatus.addReceived(bytes.size)
                handleServerJson(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                CallStreamStatus.setError(t.message ?: "WebSocket connection failed")
                cleanup(CallStreamStatus.Connection.ERROR)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                cleanup(CallStreamStatus.Connection.DISCONNECTED, closeSocket = false)
            }
        })
    }

    private fun handleServerJson(raw: String) {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: run {
            // Not JSON — treat as plain transcript text.
            appendTranscript(raw)
            LiveTranscriptBus.push(raw)
            return
        }
        json.optString("transcript_chunk").takeIf { it.isNotBlank() }?.let { chunk ->
            appendTranscript(chunk)
            CallStreamStatus.setTranscript(getFullTranscription())
            LiveTranscriptBus.push(chunk) // on-device spam-detection analyzes this
        }
        if (json.has("threat_score")) {
            val score = json.optDouble("threat_score", 0.0).toFloat()
            _threatScore.value = score
            CallStreamStatus.setServerThreat(score)
        }
        json.optString("state").takeIf { it.isNotBlank() }?.let {
            _callState.value = it
            CallStreamStatus.setServerState(it)
        }

        val warningB64 = json.optString("warning_audio")
        if (warningB64.isNotBlank()) playWarningAudio(warningB64)
    }

    private fun appendTranscript(text: String) {
        synchronized(transcriptionBuilder) {
            transcriptionBuilder.append(text).append(" ")
            _transcriptionState.value = transcriptionBuilder.toString()
        }
    }

    /** Decode base64 PCM16 warning speech from VOICE and play it back. */
    private fun playWarningAudio(base64: String) {
        scope.launch {
            var track: AudioTrack? = null
            try {
                val pcm = Base64.decode(base64, Base64.DEFAULT)
                if (pcm.isEmpty()) return@launch
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(TTS_SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(pcm.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = ((pcm.size / 2.0) / TTS_SAMPLE_RATE * 1_000).toLong()
                delay(durationMs.coerceAtLeast(250L))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play warning audio", e)
            } finally {
                track?.let { audioTrack ->
                    runCatching { audioTrack.stop() }
                    audioTrack.release()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        check(bufferSize > 0) { "Unsupported 16 kHz PCM audio configuration" }
        val captureBufferSize = bufferSize.coerceAtLeast(3200)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate, channelConfig, audioFormat, captureBufferSize,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init failed")
            record.release()
            return
        }
        audioRecord = record
        record.startRecording()
        check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            "Microphone did not enter recording state"
        }
        isRecording = true

        recordingJob = scope.launch {
            val buffer = ByteArray(captureBufferSize)
            while (isRecording) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val sent = webSocket?.send(buffer.copyOf(read).toByteString()) ?: false
                    if (sent) CallStreamStatus.addSent(read)
                } else if (read < 0) {
                    CallStreamStatus.setError("AudioRecord read failed ($read)")
                    break
                }
            }
        }
    }

    fun stopStreaming() {
        runCatching { webSocket?.send("{\"command\":\"stop\"}") }
        cleanup(CallStreamStatus.Connection.DISCONNECTED)
    }

    fun close() {
        stopStreaming()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    @Synchronized
    private fun cleanup(
        connection: CallStreamStatus.Connection,
        closeSocket: Boolean = true,
    ) {
        started.set(false)
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.let { record ->
            runCatching { if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop() }
            record.release()
        }
        audioRecord = null
        if (closeSocket) webSocket?.close(1000, "Stopped")
        webSocket = null
        CallStreamStatus.setConnection(connection)
    }

    fun getFullTranscription(): String = synchronized(transcriptionBuilder) {
        transcriptionBuilder.toString()
    }

    fun clearTranscription() {
        synchronized(transcriptionBuilder) {
            transcriptionBuilder.setLength(0)
            _transcriptionState.value = ""
        }
    }

    private companion object {
        const val TAG = "AudioStreaming"
        // Gemini TTS returns 24 kHz mono PCM16.
        const val TTS_SAMPLE_RATE = 24000
    }
}
