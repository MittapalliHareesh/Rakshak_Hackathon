# Screen 3: Active Threat Interceptor

Based on `docs/ui-reference/rakshak/active_threat_interceptor/`.

## Purpose
The critical intervention screen. This overlay appears when a high-confidence scam (Digital Arrest, Extortion) is detected during a live call. Its sole purpose is to break the victim's panic cycle and force them to hang up.

## Visual State
- **Type:** `SYSTEM_ALERT_WINDOW` (Draws over all other apps, hiding the dialer/WhatsApp).
- **Background:** Solid **Alert Red** (`#ba1a1a`). Extreme urgency.
- **Text:** White, massive, bold.

## Key UI Components

1.  **Massive Warning Header**
    -   **Icon:** Giant warning triangle or shield with an 'X'.
    -   **Text:** "SCAM DETECTED" (`headline-lg`, `32px+`).

2.  **Clear Instruction Body**
    -   **Text:** "This caller is attempting to scam you. Hang up immediately. Do not share personal details." (`callout` style, `22px+`).

3.  **The "Kill Switch" (Primary Action)**
    -   **Visual:** The largest element on the screen. A massive button.
    -   **Color:** White background, Red bold text, or distinct contrasting color.
    -   **Text:** "HANG UP NOW" (or simply an enormous Red End Call icon if technically feasible to inject the telecom command).
    -   **Size:** Minimum `80px` height, spanning 90% of the screen width.

4.  **Audio / Haptic Context**
    -   **TTS:** While this screen is visible, the loud TTS voice announces: "This is a scam. Hang up immediately."
    -   **Vibration:** Continuous, aggressive vibration pattern.

## Interaction Rules
- **No Dismiss Button:** The user cannot simply swipe this away. They *must* interact with the "Hang Up" action or use the Cheat Code. This prevents accidental dismissal while in a panic state.
- **Cheat Code Listener:** STT is actively listening for "Beta, help".