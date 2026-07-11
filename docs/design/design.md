# Rakshak Design System: "Vigilant Guardian"

This document outlines the core design principles and UI/UX specifications for Rakshak, based on the `vigilant_guardian` design system.

## 1. Brand & Persona
The design system is built on the personality of **"The Vigilant Son/Daughter"**. It acts as a digital companion that is protective, professional, and culturally resonant. 
- **Target Audience:** Elderly users in India (55+).
- **Core Emotion:** Safety and Reassurance. The UI should feel like a sturdy shield rather than a fragile digital tool.
- **Visual Style:** Corporate, Modern, High-Contrast, and Tactile. Visual noise is minimized to reduce cognitive load.

## 2. Color Palette
The palette uses semantic colors to indicate safety states clearly to aging eyes:
- **Guardian Blue (Primary - `#002045`):** Used for headers, primary buttons, and stable/idle states. Evokes authority and trust.
- **Safety Green (Secondary - `#0a6c44`):** Used for verified contacts, secure states, and the "Gentle Guidance Mode". Provides a clear visual "go" signal.
- **Alert Red (Tertiary - `#ba1a1a`):** Used strictly for high-threat interceptions, active scams, and emergency block actions.

## 3. Typography
- **Font Family:** **Plus Jakarta Sans** (friendly, geometric, highly legible).
- **Minimum Body Size:** `18px` to ensure legibility without glasses.
- **Headlines:** Bold (`700` weight) with generous line heights (1.5x) to prevent text blurring.
- **Contrast:** Strict AAA contrast ratio for all text elements.

## 4. Layout & Accessibility Rules
- **Large Touch Targets:** A fundamental rule—no interactive element (button, checkbox, card) can be smaller than **56px** in height/width.
- **Spacing:** Wide spacing (`24px` mobile margins) to prevent "fat-finger" accidental touches.
- **Single Column:** Content flows vertically to simplify the mental model.
- **Shapes:** Rounded corners (`8px` base, `16px` for primary buttons) to feel approachable.

## 5. Key UI Modes

### A. Rakshak Dashboard & Security History
- **Visuals:** Soft off-white backgrounds, Guardian Blue headers.
- **Function:** Shows system status (Monitoring Active) and a log of processed SMS and calls with clear Status Chips (Safe, Watch, Alert).

### B. Active Threat Interceptor
- **Trigger:** System confirms an active digital arrest scam with high confidence.
- **Visuals:** Full-screen **Alert Red** overlay. Hides the underlying dialer/video call.
- **UX:** Extremely authoritative. Presents a massive "HANG UP NOW" button. Accompanied by loud TTS barge-in.

### C. Gentle Guidance Mode
- **Trigger:** The elderly user says a cheat-code like "Beta, help".
- **Visuals:** Switches from Red to a calming **Safety Green / Guardian Blue** theme.
- **UX:** The tone shifts from authoritative to reassuring. Reduces panic by guiding them to press a single, safe exit button, accompanied by a soft, familiar TTS voice.