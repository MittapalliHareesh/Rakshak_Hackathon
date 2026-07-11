---
name: Vigilant Guardian
colors:
  surface: '#f7fafc'
  surface-dim: '#d7dadc'
  surface-bright: '#f7fafc'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f4f6'
  surface-container: '#ebeef0'
  surface-container-high: '#e5e9eb'
  surface-container-highest: '#e0e3e5'
  on-surface: '#181c1e'
  on-surface-variant: '#43474e'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eef1f3'
  outline: '#74777f'
  outline-variant: '#c4c6cf'
  surface-tint: '#455f88'
  primary: '#002045'
  on-primary: '#ffffff'
  primary-container: '#1a365d'
  on-primary-container: '#86a0cd'
  inverse-primary: '#adc7f7'
  secondary: '#0a6c44'
  on-secondary: '#ffffff'
  secondary-container: '#9ff5c1'
  on-secondary-container: '#167249'
  tertiary: '#4b0004'
  on-tertiary: '#ffffff'
  tertiary-container: '#73000b'
  on-tertiary-container: '#ff736b'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d6e3ff'
  primary-fixed-dim: '#adc7f7'
  on-primary-fixed: '#001b3c'
  on-primary-fixed-variant: '#2d476f'
  secondary-fixed: '#9ff5c1'
  secondary-fixed-dim: '#83d8a6'
  on-secondary-fixed: '#002111'
  on-secondary-fixed-variant: '#005231'
  tertiary-fixed: '#ffdad6'
  tertiary-fixed-dim: '#ffb3ad'
  on-tertiary-fixed: '#410003'
  on-tertiary-fixed-variant: '#920212'
  background: '#f7fafc'
  on-background: '#181c1e'
  surface-variant: '#e0e3e5'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 30px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '700'
    lineHeight: 24px
    letterSpacing: 0.05em
  callout:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 32px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  touch-target-min: 56px
  gutter: 20px
  margin-mobile: 24px
  margin-desktop: 64px
  stack-sm: 12px
  stack-md: 24px
  stack-lg: 40px
---

## Brand & Style

The design system is built on the personality of "The Vigilant Son/Daughter"—a digital companion that is protective, professional, and culturally resonant. The target audience consists of elderly users in India who require clarity and reassurance in a complex digital landscape.

The design style is **Corporate / Modern** with a focus on extreme legibility and tactile feedback. It avoids thin lines and complex layers in favor of solid blocks of color and high-contrast elements. The emotional response is one of safety; the UI should feel like a sturdy shield rather than a fragile tool. Visual noise is minimized to reduce cognitive load, ensuring that the primary action is always the most obvious one.

## Colors

The palette is anchored by **Guardian Blue**, a deep, reliable shade that evokes authority and trust. **Safety Green** is used for guidance and "Safe" states, providing a clear visual "go" signal. **Alert Red** is reserved strictly for high-threat interceptions and blocking actions.

To ensure accessibility for aging eyes, the design system utilizes a high-contrast ratio for all text elements. The background remains a soft, off-white to prevent screen glare, while semantic colors use slightly darkened hues to ensure legibility against light backgrounds.

- **Guardian Blue (Primary):** Used for headers, primary buttons, and critical branding.
- **Safety Green (Secondary/Safe):** Used for verified contacts and secure states.
- **Status Yellow (Watch):** Used for unknown callers or unverified links.
- **Status Orange (Alert):** Used for suspicious activity or reported numbers.
- **Alert Red (Tertiary/Block):** Used for active scams and emergency actions.

## Typography

The design system uses **Plus Jakarta Sans** for its friendly yet professional geometry and exceptional legibility at large scales. Recognizing the visual needs of the elderly, the minimum body font size is set to 18px.

Headlines are bold and distinctive to allow for quick scanning of screen intent. Line heights are generous (1.5x) to prevent lines of text from blurring together. Text contrast is strictly maintained at AAA levels for all primary content. For critical warnings, a specific `callout` style is used to ensure instructions are impossible to miss.

## Layout & Spacing

The layout follows a **fluid grid** model with exaggerated margins to ensure content feels contained and safe. On mobile devices, a 24px side margin is enforced to prevent accidental edge-touches.

A fundamental rule of this design system is the **Large Touch Target** principle. No interactive element (button, checkbox, or card) should have a height or width smaller than 56px. Spacing between interactive elements (stack-md) is kept wide to prevent "fat-finger" errors, which are common in the target demographic. Content is presented in a single-column vertical flow as much as possible to simplify the navigation mental model.

## Elevation & Depth

To provide clear mental models of what is "on top" or "pressable," the design system uses **Tonal Layers** combined with **Ambient Shadows**. 

Surfaces are categorized into three levels:
1. **Base (Level 0):** The soft neutral background.
2. **Card (Level 1):** Solid white containers with a subtle 1px border (#E2E8F0) and a soft, wide shadow to indicate they can be interacted with.
3. **Overlay (Level 2):** Critical alerts or bottom sheets, using a backdrop dimming effect (60% opacity) to focus the user’s attention entirely on the protection task at hand.

Shadows are not purely black; they are tinted with the Primary Guardian Blue to maintain a cohesive, warm atmosphere.

## Shapes

The shape language uses **Rounded** corners (0.5rem / 8px base) to feel approachable and modern, avoiding the "sharpness" of institutional software. 

- **Primary Buttons:** Use a more pronounced `rounded-lg` (16px) to look more like physical, pressable buttons.
- **Status Indicators:** Small status pips use full circles, while card containers use the standard 8px radius.
- **Cultural Texture:** Subtle, low-opacity (2%) geometric patterns inspired by Indian *Jaali* or *Mandala* motifs can be used in the background of Guardian Blue headers to add a sense of cultural familiarity and warmth.

## Components

### Buttons
Buttons are the core of the interceptor experience. They must be at least 56px tall. The **Primary Action** button uses Guardian Blue with white text. **Secondary Actions** use a thick 2px outline. High-threat actions (e.g., "Block Caller") use a solid Alert Red background.

### Cards
Cards are the primary way to display scam alerts. They include a large icon on the left (Safe, Watch, or Alert) followed by clear, bolded text. The entire card area is a touch target.

### Status Chips
Status chips are used to label phone numbers. They use high-saturation backgrounds (Green, Yellow, Orange, Red) with high-contrast black or white text. Labels must be written in simple, direct language (e.g., "Safe," "Scam," "Unknown").

### Input Fields
Inputs use a 18px font size and a 2px border when focused to provide clear visual feedback. Labels are always persistent (never placeholder-only) to ensure the user knows what information they are providing.

### Iconography
Icons should be thick-stroked (2pt minimum) and literal. Use familiar symbols: a "Shield" for protection, a "Hand" for stopping/blocking, and a "Tick" for safety. Avoid abstract metaphors. Where possible, include bilingual labels (English and the local Indian language) within or adjacent to critical components.