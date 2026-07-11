# Screen 2: Security History

Based on `docs/ui-reference/rakshak/security_history/`.

## Purpose
A detailed log of all communication scanned by Rakshak. It provides transparency to the user and their family members about what threats were blocked.

## Visual State
- **Background:** Soft off-white (`surface` / `#f7fafc`).
- **Header:** White or light gray, with a clear "Back" button (large touch target).

## Key UI Components

1.  **Filter Tabs (Optional for Hackathon)**
    -   Large touch targets to filter by "All", "Calls", "SMS", "Threats Blocked".

2.  **History List Items (Cards)**
    -   Each log entry is a distinct card (`elevation-1`, soft shadow, `8px` rounded).
    -   **Layout:**
        -   Left: Large status icon (Safe, Watch, Alert, Blocked).
        -   Middle: Bold title (Caller ID or "Unknown Number"), Subtitle (Timestamp).
        -   Right: Clear status text ("Blocked", "Scanned").
    -   **Status Color Coding (Crucial):**
        -   **Safe:** Green tick.
        -   **Watch (Suspicious):** Yellow/Orange warning icon.
        -   **Blocked (Scam):** Red stop/shield icon.

3.  **Expandable Detail (Accordion)**
    -   Tapping a card expands it to show more info (e.g., the extracted transcript snippet that triggered the block, or the phishing URL found in an SMS).

## Interaction Rules
- Focus on chronological clarity.
- Tapping an item provides more details without navigating away (reduces cognitive load of navigating deep hierarchies).