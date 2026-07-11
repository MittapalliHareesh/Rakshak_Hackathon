# Screen 5: Onboarding & Permissions

## Purpose
Rakshak requires highly sensitive permissions (Microphone, SMS, System Alert Window, Location). Standard Android permission popups are terrifying and confusing for elderly users. This screen acts as a "Trust Builder," explaining *why* each permission is needed in plain, reassuring language before the OS popup appears.

## Visual State
- **Background:** `surface` (Soft off-white).
- **Header:** "Let's set up your Guardian." (Guardian Blue).
- **Progress:** A simple 1-of-4 step indicator.

## Key UI Components

1.  **Permission Explanation Card**
    -   **Icon:** Friendly, large, contextual icon (e.g., a Microphone for audio).
    -   **Headline:** "Listening for Threats"
    -   **Body Text:** "Rakshak needs access to your microphone to listen during phone calls. It only listens for scammers and never records your private conversations." (Minimum 18px).
    
2.  **Action Buttons**
    -   **Primary:** "Allow Microphone" (Guardian Blue, large 56px+ target). Tapping this triggers the actual Android permission prompt.
    -   **Secondary:** "Why do you need this?" (Expandable text for more details).

3.  **Permission Sequence**
    -   Step 1: Microphone (`RECORD_AUDIO`)
    -   Step 2: Display Over Apps (`SYSTEM_ALERT_WINDOW` - "To block your screen when a scammer attacks").
    -   Step 3: SMS & Contacts (`RECEIVE_SMS`, `SEND_SMS`, `READ_CONTACTS` - "To scan messages and alert your family").
    -   Step 4: Location (`ACCESS_FINE_LOCATION` - "To tell your family where you are in an emergency").

## Interaction Rules
- Only ask for one permission at a time.
- If a permission is denied, show a gentle, persistent warning explaining that Rakshak cannot protect them without it, offering a button to go to Android Settings.