# Screen 7: Live Detection & Demo Display

## Purpose
While the Orchestrator runs invisibly in the background for the elderly user, we need a **Live Detection Display** for two reasons:
1.  **Hackathon Demo:** To show the judges the dual-pipeline AI working in real-time (transcribing, scoring, and fusing data) *before* the red intervention screen hits.
2.  **User Transparency:** An optional "Active Scan" notification/widget the user can open to see what the AI is currently transcribing.

## Visual State
- **Background:** Dark mode preferred (`inverse-surface` / `#2d3133`) to look like a high-tech monitoring dashboard for the judges.
- **Typography:** Monospace font for the transcript, large bold numbers for the Threat Score.

## Key UI Components

1.  **Live Transcript Feed**
    -   A scrolling text box displaying the `SharedFlow<String>` from the STT engine.
    -   **Highlighting:** When the Spam Detector detects a trigger word (e.g., "Aadhaar", "CBI", "Arrest"), it highlights that specific word in **Status Yellow** or **Alert Red** in the transcript in real-time.

2.  **Dual Threat Score Meters**
    -   Two horizontal progress bars (0-100 scale).
    -   **Meter 1:** Gemma 4 (On-Device) Score. (e.g., `45/100`)
    -   **Meter 2:** Gemini Live (Cloud) Score. (e.g., `82/100`)
    -   **Meter 3 (Giant):** Fusion Engine Final Score. Changes color: Green (<30) -> Yellow (31-50) -> Orange (51-75) -> Red (>75).

3.  **Active State Indicator**
    -   Displays the current Orchestrator State: `MONITORING` -> `ALERT` -> `ACTIVE_THREAT`.

## Interaction Rules
- **Demo Flow:** This screen is projected/shown to the judges. The mock audio plays, the transcript scrolls, the words highlight, the bars fill up, and when the Fusion Score hits >75, this screen is violently overridden by the **Screen 3: Active Threat Interceptor** (Red Screen).
- The transition from this analytical dashboard to the panic-breaking red screen is the climax of the demo.