# Rakshak: Digital Arrest Extortion Interceptor

**Rakshak** is a hybrid AI-powered Android application designed to protect elderly citizens from "Digital Arrest" and extortion scams. It monitors communication channels in real-time, using a combination of on-device and cloud-based AI to detect threats and intervene before financial harm occurs.

---

## 🚀 The Problem: Digital Arrest Scams
"Digital Arrest" is a rising cybercrime where scammers impersonate law enforcement (CBI, Customs, Police) via voice calls or messages, claiming the victim is under "investigation." They use psychological pressure to keep victims isolated and extort large sums of money. Elderly citizens are particularly vulnerable to these high-pressure tactics.

## ✨ Key Features
- **Real-Time Call Monitoring**: Transcribes and analyzes live voice calls to detect scam patterns.
- **SMS Threat Detection**: Automatically scans incoming SMS for fraudulent links and extortion language.
- **Hybrid AI Architecture**:
    - **On-Device (Gemma 2b)**: Provides 100% offline detection for privacy and low-connectivity scenarios.
    - **Cloud (Gemini Live API)**: Performs deep semantic analysis and multi-language reasoning.
- **Panic-Breaking UI Overlays**: When a high threat is detected, Rakshak seizes the screen with an un-dismissible high-contrast overlay. This hides the scammer's visual/audio hold and provides a clear, massive button to terminate the call.
- **Vocal Interventions (TTS Barge-in)**: The app literally speaks over the scammer, telling the user: "This is a scam. Hang up immediately." This helps break the psychological "spell" scammers cast.
- **"Gentle Mode" Safety Phrase**: If the aggressive warning screen is too startling, the user can say a safety phrase (e.g., "Beta, help"), and the app switches to a calming green screen with a familiar voice to guide them safely out of the call.
- **Evidence Export & Reporting**: Automatically generates a detailed JSON evidence report (compatible with cybercrime.gov.in) containing call transcripts, threat analysis, and metadata to assist in legal action.
- **Family Emergency Alerts**: Automatically notifies family members via SMS with the victim's location when a high-threat scam is detected.
- **Multi-Language Support**: Support for major Indian languages including Hindi, Telugu, Tamil, Bengali, Marathi, and more.

## 🛠 Tech Stack
- **Android App**: Kotlin, Jetpack Compose, Coroutines/Flow, Hilt (Dependency Injection).
- **On-Device AI**: Google Gemma 2b via MediaPipe.
- **Backend Service**: Python (FastAPI), Google Gemini 2.0 Flash (Live & TTS Preview).
- **Communication**: WebSockets for real-time streaming, REST APIs.

## 📂 Project Structure
- `app/`: The core Android application source code.
- `VOICE/`: Python backend providing Gemini-powered STT, TTS, and advanced threat analysis.
- `docs/`: Detailed architecture diagrams, design specs, and module breakdowns.

## ⚙️ Setup Instructions

### 1. Backend (Python)
1. Navigate to the `VOICE/` directory.
2. Create a virtual environment: `python -m venv .venv` and activate it.
3. Install dependencies: `pip install -r requirements.txt` (or use `uv sync`).
4. Create a `.env` file and add your `GEMINI_API_KEY`.
5. Run the server: `uvicorn app.main:app --host 0.0.0.0 --port 8000`.

### 2. Android App
1. Open the project in Android Studio.
2. In `local.properties`, set your API key and backend URL:
   ```properties
   GEMINI_API_KEY="your_actual_gemini_api_key"
   VOICE_WS_BASE="ws://YOUR_IP_ADDRESS:8000"
   ```
   *Note: If using an emulator, you can use `ws://10.0.2.2:8000`.*
3. Build and run the `app` module on an Android device or emulator.
4. Grant the necessary permissions (Record Audio, SMS, Overlay, etc.) when prompted.

---

## 🛡 Security & Privacy
Rakshak is designed with privacy-first principles. The on-device Gemma model ensures that sensitive conversation data can be analyzed without ever leaving the phone, providing a fallback layer even without internet access.

<img width="1280" height="714" alt="image" src="https://github.com/user-attachments/assets/4508627e-c53d-4c37-bd6c-69a0170485cd" />




Demo URL:- https://drive.google.com/drive/folders/1VDxlg2rf0zpL7QLpAnyioDLzGCme5fDR

