# Rakshak - Product Requirements Document (PRD)

## 1. Product Vision
**Rakshak** (meaning "Protector") is an autonomous, on-device Guardian Angel designed to protect India's elderly from sophisticated, prolonged cyber extortion, specifically "Digital Arrest" scams. 

Unlike standard spam blockers that just check phone numbers against a database, Rakshak actively listens to the conversation, understands the manipulative narrative arc of a scam, and intervenes in real-time.

## 2. The Problem
- **Digital Arrest Scams:** Scammers impersonate authority figures (CBI, Police, Customs) over voice or video calls. They isolate the victim, induce panic, claim their Aadhaar or bank account is linked to a crime, and coerce them into transferring funds.
- **The Target:** Elderly citizens (55+) who are less tech-savvy, easily intimidated by "digital" authority, and often live alone.
- **The Gap:** Current solutions require the user to realize they are being scammed and hang up. In a panic state, victims fail to do this. We need an app that breaks the panic cycle *for* them.

## 3. Core Features

### Feature 1: Real-Time Dual-Pipeline Interception
- Listens to active phone calls in the background.
- Uses **Gemma 4 E2B (On-Device)** for immediate, offline keyword/pattern detection (e.g., "Aadhaar", "CBI", "Digital Arrest").
- Uses **Gemini Live API (Cloud)** for deep semantic understanding of the call's narrative arc and dynamic audio barge-in.

### Feature 2: Panic-Breaking UI Overlays (System Alert Window)
- When a threat is confirmed, Rakshak seizes the screen. It draws an un-dismissible overlay on top of the dialer or WhatsApp video call.
- Hides the scammer's face/voice and replaces it with a massive, high-contrast button to terminate the call.

### Feature 3: Vocal Interventions (TTS Barge-in)
- The app speaks over the scammer: "This is a scam. Hang up immediately." This breaks the scammer's psychological hold over the victim.

### Feature 4: "Gentle Mode" Cheat Code
- If the victim is terrified by the aggressive red warning screen, they can say a safety phrase like **"Beta, help"**.
- The app immediately switches to "Gentle Guidance Mode"—the screen turns calming green, and a soft, familiar voice reassures them and guides them to hang up.

### Feature 5: Automated Family SOS
- The moment a high-threat block occurs, Rakshak automatically sends an SMS/WhatsApp to pre-configured family members with the victim's GPS location and a transcript snippet of the scam attempt.

### Feature 6: Proactive SMS Monitoring
- Silently scans incoming SMS for phishing links and urgency triggers (e.g., "Electricity cut off tonight, click here"), warning the user before they even initiate a call.

## 4. Hackathon Success Metrics (The "Wow" Factor)
For the one-day hackathon presentation, success is defined by:
1.  **Fast Audience Pickup:** The judges instantly understand the product when the app interrupts a live (mocked) scam call with its TTS voice and Red Overlay.
2.  **Live API Utilization:** Proving we use Gemini Live for *audio-to-audio* barge-in, not just a text chat wrapper.
3.  **Local-First Resilience:** Demonstrating that if we turn off Wi-Fi, the Gemma 4 model still catches the scam and triggers the UI.