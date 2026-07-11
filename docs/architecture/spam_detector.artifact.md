# Spam Detector (Hybrid Threat Engine)

The Spam Detector is the core intelligence of Rakshak. It utilizes a dual-model architecture to guarantee both extreme speed/privacy (Gemma) and deep contextual understanding (Gemini).

## Plug-and-Play Integration
Both models implement a unified `ThreatAnalyzer` interface:
```kotlin
interface ThreatAnalyzer {
    suspend fun analyze(context: CallContext): ThreatScore
}
```
The `ThreatFusionEngine` takes a `List<ThreatAnalyzer>` injected via Dagger/Hilt. To add a new model (e.g., a rule-based regex engine), you simply create a new class implementing `ThreatAnalyzer` and add it to the Dagger module.

## Implementation Details & Usage

### 1. Gemma 4 On-Device (Offline First)
- **Integration**: Uses `MediaPipe LLM Inference API`.
- **Setup**: The `.task` file (Gemma 4 E2B) is stored in the app's `assets/` or downloaded on first run.
- **Context Handling**: Maintains a rolling window of the last 10 conversational turns to preserve memory while maintaining context.
- **Why**: Zero latency, works completely offline.

### 2. Gemini Live API (Online)
- **Integration**: Uses `OkHttp WebSocket` to connect to `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent`.
- **Setup**: API Key is injected via `BuildConfig.GEMINI_API_KEY`.
- **Context Handling**: Gemini Live natively holds state across the WebSocket session. We send raw PCM audio chunks continuously.

## Fault Tolerance: Cloud to Local Fallback
The `ThreatFusionEngine` scores threats using: `(Gemma * 0.4) + (Gemini * 0.6)`.
- **Network Drop**: If the WebSocket drops or returns a 5xx error, the Fusion Engine catches the exception, marks the Gemini score as `null`, and dynamically shifts the formula to `(Gemma * 1.0)`.
- The user is seamlessly protected by the local model without any app crash or interruption.

## Required Permissions
```xml
<!-- Required for Gemini Live API -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```