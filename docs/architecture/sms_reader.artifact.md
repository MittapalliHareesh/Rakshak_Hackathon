# SMS Reader Module

The SMS Reader Module acts as the first line of defense, monitoring incoming SMS messages for phishing links, APK downloads, and urgency-driven scam narratives.

## Plug-and-Play Integration
The module is designed as a standalone receiver that pipes data into a unified `MessageFlow`.
```kotlin
interface SmsInterceptor {
    fun startListening()
    val incomingMessages: SharedFlow<SmsPayload>
}
```
Because of this design, the Spam Detector doesn't care if the text came from an SMS, WhatsApp (via Accessibility service in the future), or a test mock. It just observes `incomingMessages`.

## Technical Architecture
1.  **Broadcast Receiver**: `SmsReceiver` extending `BroadcastReceiver`. Listens for `android.provider.Telephony.SMS_RECEIVED`.
2.  **Processing Pipeline**: Extracts PDUs, normalizes text (handling obfuscations like "C L 1 C K"), and forwards to the Spam Detector.
3.  **UI/UX**: Logs to `security_history`.

## Fault Tolerance
- If the receiver crashes, the Android OS will automatically restart it on the next incoming SMS because it is registered statically in the `AndroidManifest.xml`.
- If SMS parsing fails (e.g., malformed PDU), the module logs the error and gracefully drops the message without crashing the main Orchestrator.

## Required Permissions
```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
```