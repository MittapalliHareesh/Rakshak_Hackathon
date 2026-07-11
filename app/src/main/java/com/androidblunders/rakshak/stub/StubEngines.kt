package com.androidblunders.rakshak.stub

import android.util.Log
import com.androidblunders.rakshak.core.contract.SpeechToTextEngine
import com.androidblunders.rakshak.core.contract.TextToSpeechEngine
import com.androidblunders.rakshak.core.contract.ThreatResponder
import com.androidblunders.rakshak.core.model.Priority
import com.androidblunders.rakshak.core.model.ThreatLevel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Placeholder wiring so the orchestrator graph is complete and runnable BEFORE
 * the real STT / TTS / overlay modules exist. Each is a drop-in that the future
 * module replaces by rebinding its interface in a Hilt module. Nothing else changes.
 */

/** No-op STT. Real modules: GeminiLiveStt / AndroidSpeechRecognizerStt. */
@Singleton
class NoOpSpeechToText @Inject constructor() : SpeechToTextEngine {
    private val _transcriptions = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val transcriptions: SharedFlow<String> = _transcriptions.asSharedFlow()

    /** Test hook: push a fake transcript into the pipeline from the demo UI. */
    fun emit(text: String) {
        _transcriptions.tryEmit(text)
    }

    override fun start() { Log.d(TAG, "NoOpSpeechToText.start()") }
    override fun stop() { Log.d(TAG, "NoOpSpeechToText.stop()") }

    private companion object { const val TAG = "NoOpStt" }
}

/** Logs interventions instead of speaking. Real module: GeminiLiveTts + AndroidNativeTts. */
@Singleton
class LoggingTextToSpeech @Inject constructor() : TextToSpeechEngine {
    override suspend fun speak(text: String, priority: Priority) {
        Log.i(TAG, "[$priority] TTS -> \"$text\"")
    }
    override fun stop() {}
    private companion object { const val TAG = "LoggingTts" }
}

/**
 * Default responder: logs every threat-level transition and speaks the canned
 * intervention line via the TTS engine. The real overlay responder
 * (SYSTEM_ALERT_WINDOW) is added @IntoSet alongside this later.
 */
@Singleton
class LoggingThreatResponder @Inject constructor(
    private val tts: TextToSpeechEngine,
) : ThreatResponder {
    override suspend fun onThreatLevel(level: ThreatLevel) {
        Log.i(TAG, "Threat level -> $level")
        when (level) {
            ThreatLevel.ACTIVE_THREAT ->
                tts.speak("This looks like a scam. Do not share any details. Hang up now.", Priority.CRITICAL)
            ThreatLevel.EMERGENCY ->
                tts.speak("Danger. Alerting your family now.", Priority.CRITICAL)
            ThreatLevel.GENTLE_GUIDANCE ->
                tts.speak("Beta, I am here. Let us hang up together. Press the green button.", Priority.NORMAL)
            ThreatLevel.MEDIUM ->
                tts.speak("Please be careful. This message may be unsafe.", Priority.LOW)
            ThreatLevel.LOW, ThreatLevel.IDLE -> Unit
        }
    }
    private companion object { const val TAG = "ThreatResponder" }
}
