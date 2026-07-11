# Screen 1: Rakshak Dashboard

Based on `docs/ui-reference/rakshak/rakshak_dashboard/`.

## Purpose
The primary home screen of the application. It provides immediate reassurance that the system is active and monitoring, while offering quick access to settings and recent history.

## Visual State
- **Background:** Soft off-white (`surface` / `#f7fafc`).
- **Header:** **Guardian Blue** (`#002045`) with white text. Clean, authoritative, yet approachable.

## Key UI Components

1.  **Status Card (Hero Element)**
    -   **Visual:** A large, prominent card at the top.
    -   **State: Active:** Displays a pulsing **Safety Green** shield icon.
    -   **Text:** "Rakshak is Active" (Headline) / "Monitoring calls and messages for your safety." (Body).
    -   **Action:** Includes a toggle switch to easily disable monitoring (requires a confirmation dialog to prevent accidental disable).

2.  **Quick Actions Grid**
    -   Large, highly legible buttons (min 56px height, rounded corners `8px`).
    -   **Action 1:** "Manage Trusted Contacts" (Icon: Family/Group).
    -   **Action 2:** "View Security History" (Icon: Shield with a clock/check). Navigate to Screen 2.

3.  **Recent Activity Snapshot**
    -   A simplified list view of the 2-3 most recent events.
    -   Uses **Status Chips** to indicate event type.
        -   [Green Shield] "Call from Ravi (Son) - Safe"
        -   [Yellow Warning] "Unknown SMS - Scanned (Safe)"

## Interaction Rules
- All text must be minimum `18px` (Body) to `24px+` (Headers).
- High contrast (AAA) enforced. No light gray text on white backgrounds.
- Single-column scrolling.