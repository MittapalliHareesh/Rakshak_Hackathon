# Live Call Transcriber Module

The Call Transcriber is responsible for continuous, low-latency audio capture during an active phone call. It acts as the sensory input for both the local Gemma 4 model and the Gemini Live API.

## Plug-and-Play Integration
Audio capture is completely decoupled from analysis. It exposes a continuous stream of byte arrays:
```kotlin
interface AudioStreamer {
    fun startStreaming(): Flow<ByteArray>
    fun stopStreaming()
}
```
This allows us to instantly swap between:
- `RealCallStreamer`: Uses `AudioSource.VOICE_COMMUNICATION`.
- `MicMockStreamer`: Uses `AudioSource.MIC` (for the hackathon demo).
- `FileMockStreamer`: Streams from a local `.wav` file for unit testing.

## Technical Details
- **Format**: 16kHz, Mono, 16-bit PCM (Strict requirement for Gemini Live API).
- **Service**: Runs inside a Foreground Service to prevent Android from killing the microphone access while the app is in the background.

## Fault Tolerance & Fallbacks
- **Mic Contention**: If another app takes exclusive control of the microphone, the `AudioStreamer` catches the `AudioRecord` failure, attempts to restart every 5 seconds, and notifies the Orchestrator to display a "Protection Suspended" warning.
- **Buffer Overflows**: Uses a circular buffer. If the AI models are processing too slowly, oldest audio chunks are dropped rather than crashing the app with an OOM (Out of Memory) error.

## Required Permissions
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```