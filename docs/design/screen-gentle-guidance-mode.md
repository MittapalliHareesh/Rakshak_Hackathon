# Screen 4: Gentle Guidance Mode

Based on `docs/ui-reference/rakshak/gentle_guidance_mode/`.

## Purpose
The "Friendly Override". If the user is overwhelmed by the Active Threat Interceptor (Red Screen) and says the cheat phrase ("Beta, help" / "Mujhe madad chahiye"), the UI instantly transforms to calm them down and guide them to safety.

## Visual State
- **Type:** `SYSTEM_ALERT_WINDOW` (Replaces the Red overlay).
- **Background:** Calming **Safety Green** (`#0a6c44`) or soft **Guardian Blue**.
- **Text:** White, soft, reassuring.

## Key UI Components

1.  **Reassuring Header**
    -   **Icon:** A gentle, protective icon (e.g., hands holding a shield, or a familiar avatar).
    -   **Text:** "You are safe." (`headline-lg`, `32px+`).

2.  **Guided Instruction Body**
    -   **Text:** "We have blocked the threat. Please press the red button below to end the call safely." (`body-lg`, `20px+`).

3.  **Guided Action Button**
    -   **Visual:** A clearly defined, standard-looking "End Call" button (Red circle with a white phone down icon).
    -   **Size:** Large touch target (`72px+`), placed exactly where they expect it.

4.  **Audio Context**
    -   **TTS:** The voice changes to a softer, slower, familiar profile. "Dadiji, it's okay. You are safe. Please press the red button on your screen to cut the call."

## Interaction Rules
- The goal is to lower the heart rate. Eliminate all flashing animations, aggressive vibrations, or stark contrasting colors found in the Active Threat screen.
- The action remains the same (terminate the call), but the framing changes from "Emergency" to "Guided Assistance".