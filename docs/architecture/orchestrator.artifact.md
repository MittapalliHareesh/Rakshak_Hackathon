# Main Orchestrator

The Orchestrator manages the application state and coordinates responses across all modules. It is the central nervous system of Rakshak.

## Plug-and-Play Integration
The Orchestrator observes a single source of truth:
```kotlin
val threatState: StateFlow<ThreatLevel> = fusionEngine.currentThreatLevel
```
UI components (Overlays, Activities) simply observe this `StateFlow`. If the state changes to `ACTIVE_THREAT`, the UI reacts automatically.

## Fault Tolerance & Edge Cases
- **UI Crash Recovery**: If the `SYSTEM_ALERT_WINDOW` overlay fails to draw (e.g., due to ROM specific restrictions like MIUI/ColorOS), the Orchestrator catches the `WindowManager.BadTokenException` and falls back to triggering a loud full-screen `Intent` Activity and aggressive vibrations.
- **Family Alert Failure**: If the device has no cellular signal to send the emergency SMS, the request is queued in a local Room database and dispatched immediately upon signal restoration via Android `WorkManager`.

## State Machine
- **IDLE**: Background monitoring.
- **ACTIVE_THREAT**: Red Overlay (`active_threat_interceptor`) + Aggressive TTS barge-in.
- **GENTLE_GUIDANCE**: Green/Blue Overlay (`gentle_guidance_mode`) + Soft TTS. Triggered by cheat-code ("Beta, help").

## Required Permissions
The Orchestrator requires the highest level of permissions to execute interventions:
```xml
<!-- For drawing the protective UI over the scammer's call screen -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- For sending automated emergency alerts -->
<uses-permission android:name="android.permission.SEND_SMS" />

<!-- For attaching location to emergency alerts -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- For tactile warnings -->
<uses-permission android:name="android.permission.VIBRATE" />
```