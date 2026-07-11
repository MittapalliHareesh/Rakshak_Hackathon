# STT and TTS Module (Voice Interface)

This module handles the translation of speech to text (for local analysis) and text to speech (for active intervention).

## Plug-and-Play Integration
```kotlin
interface SpeechToTextEngine {
    val transcriptions: SharedFlow<String>
}

interface TextToSpeechEngine {
    suspend fun speak(text: String, priority: Priority)
    fun stop()
}
```

## Fault Tolerance: The TTS Fallback Chain
A core requirement is that interventions *must* be heard. If the network drops during an intervention, the system guarantees delivery via fallbacks:

1.  **Primary TTS (Gemini Live)**: If the WebSocket is active, Gemini Live generates natural, contextual audio barge-ins.
2.  **Fallback (Android Native TTS)**: If the WebSocket is disconnected, or if Gemini Live times out (>800ms), the `TextToSpeechEngine` instantly falls back to `android.speech.tts.TextToSpeech`.
    - Native TTS is pre-initialized on boot with standard offline voices (English/Hindi).
    - It uses pre-baked strings: "This is a scam. Hang up immediately."

## Speech-to-Text (STT) Fallbacks
1. **Primary (Gemini Live)**: Native audio-in to text-out via WebSocket.
2. **Fallback (Android SpeechRecognizer)**: If offline, audio is routed to local `android.speech.SpeechRecognizer` to generate text for the local Gemma 4 model.

## Required Permissions
```xml
<!-- STT requires microphone access -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<!-- TTS requires no specific permissions, but requires AudioFocus management -->
```