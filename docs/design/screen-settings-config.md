# Screen 6: Configuration & Trusted Contacts

## Purpose
Allows the user (or their tech-savvy children) to configure how Rakshak responds to threats and who gets notified during an emergency.

## Visual State
- **Background:** `surface` (Soft off-white).
- **Header:** "Settings" with a back button.

## Key UI Components

1.  **Emergency Contacts Section**
    -   **List:** Displays currently added family members/trusted contacts.
    -   **Action:** "Add Trusted Contact" button (Large touch target).
    -   **Function:** These are the numbers that receive the automated SMS + Location + Transcript alert when the `ACTIVE_THREAT` state is triggered.

2.  **Protection Toggles**
    -   Large, highly visible toggle switches.
    -   "Live Call Protection" (On/Off).
    -   "SMS Phishing Protection" (On/Off).
    -   "Send Emergency Alerts to Family" (On/Off).

3.  **Voice & Language Settings**
    -   Dropdown or large radio buttons for TTS Language: "English", "Hindi", "Hinglish".
    -   "Cheat Code" configuration (Displays the current phrase: "Beta, help").

## Interaction Rules
- Changing critical settings (like disabling protection) should require a simple confirmation dialog: "Are you sure you want to turn off call protection?" (Safety Green "Keep On" vs. Outline "Turn Off").