package com.androidblunders.rakshak.services.responder

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import com.androidblunders.rakshak.core.contract.TextToSpeechEngine
import com.androidblunders.rakshak.core.model.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Offline Android TTS fallback used for every threshold-driven intervention. */
@Singleton
class AndroidTextToSpeechEngine @Inject constructor(
    @ApplicationContext context: Context,
) : TextToSpeechEngine {
    private val initialized = CompletableDeferred<Boolean>()
    private val tts = TextToSpeech(context.applicationContext) { status ->
        initialized.complete(status == TextToSpeech.SUCCESS)
    }.apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    override suspend fun speak(text: String, priority: Priority) {
        if (text.isBlank()) return
        val ready = withTimeoutOrNull(INIT_TIMEOUT_MS) { initialized.await() } == true
        if (!ready) {
            Log.e(TAG, "Android TTS failed to initialize")
            return
        }

        val preferred = Locale.forLanguageTag("en-IN")
        if (tts.isLanguageAvailable(preferred) >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = preferred
        }
        val queueMode = if (priority == Priority.CRITICAL) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }
        val result = tts.speak(text, queueMode, null, UUID.randomUUID().toString())
        if (result == TextToSpeech.ERROR) Log.e(TAG, "Android TTS rejected speech request")
    }

    override fun stop() {
        tts.stop()
    }

    private companion object {
        const val TAG = "AndroidTts"
        const val INIT_TIMEOUT_MS = 5_000L
    }
}
