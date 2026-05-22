# 🎨 Honeytong Design System

This document defines the visual and interaction design system for Honeytong.

The purpose is:
- ensure consistent UI across the entire product
- guide AI-generated UI output
- prevent design drift
- support mobile-first responsive design

---

## 1. Design Philosophy

Honeytong is not a generic review app.

It is:
- exploration-driven
- playful
- trust-based
- location-centered

Core concept:
👉 "A bee exploring flowers to collect honey"

---

## 2. Brand Identity

### Keywords

- exploration
- discovery
- honey
- local
- playful
- game-like

---

### Tone

- warm
- friendly
- soft
- inviting
- slightly playful

Avoid:
- cold
- corporate
- overly minimal
- aggressive contrast

---

## 3. Color System

### Primary Colors

| Name | Value |
|------|------|
| Honey Yellow | #FFC83D |
| Deep Honey | #FFB300 |
| Golden Accent | #FF9F1C |

---

### Secondary Colors

| Name | Value |
|------|------|
| Bee Brown | #3B2F2F |
| Warm Brown | #5A4632 |
| Cream Background | #FFF8E1 |

---

### Status Colors

| Name | Value |
|------|------|
| Success | #4CAF50 |
| Warning | #FF9800 |
| Error | #F44336 |

---

### Rules

- Do NOT use pure black (#000000)
- Do NOT use Kakao yellow (#FEE500)
- Maintain warm tone
- Avoid high contrast black-yellow combination

---

## 4. Typography

### Font

Recommended:
- Pretendard
- Noto Sans KR

---

### Hierarchy

| Type | Style |
|------|------|
| Title | Bold |
| Body | Regular |
| Caption | Light |

---

### Rules

- UI text must be Korean-first
- Keep text short on mobile
- Avoid long sentences in UI

---

## 5. Layout System

### Mobile First

Design must start from mobile.

Desktop is an extension, not a redesign.

---

### Breakpoints

| Device | Width |
|--------|------|
| Mobile | 0 ~ 767px |
| Tablet | 768 ~ 1023px |
| Desktop | 1024px+ |

---

### Grid Rules

- Mobile: 1 column
- Tablet: 2 columns
- Desktop: 3~4 columns

---

## 6. Core UI Components

---

### Buttons

#### Style

- rounded corners (12~16px)
- soft shadow
- warm color

#### Types

Primary:
- Honey Yellow background

Secondary:
- light background + border

Disabled:
- greyed out + low contrast

---

### Cards

#### Style

- rounded corners
- soft shadow
- floating feel

#### Usage

- place list
- ranking items
- user stats

---

### Icons

#### Style

- rounded
- soft
- cartoon-like

#### Theme

- bee
- flower
- honey
- hive

---

## 7. Interaction Design

---

### Core Interactions

#### Bee Motion

- actions trigger bee animation
- movement gives feedback

---

#### Flower State

- more activity → bloom
- less activity → fade

---

#### Honey Gauge

- exp gain → honey fills up

---

### Rules

- animations must be subtle
- avoid heavy motion
- feedback must be immediate

---

## 8. Responsive Design Rules

---

### Principles

- mobile-first
- layout changes, not scaling
- desktop shows more information

---

### Behavior Differences

| Element | Mobile | Desktop |
|--------|--------|--------|
| Navigation | bottom tabs | sidebar/top |
| Interaction | touch | click/hover |
| Layout | stacked | split |

---

### Examples

#### Map Screen

Mobile:
- full map
- bottom sheet

Desktop:
- map + list

---

#### Place Detail

Mobile:
- vertical scroll

Desktop:
- left image + right info

---

## 9. Navigation

---

### Mobile

- bottom navigation bar
- 4~5 main tabs

---

### Desktop

- sidebar or top navigation
- expanded options

---

## 10. Content Style

---

### Writing Style

- short
- clear
- friendly
- conversational

---

### Example

❌ "Submit your recommendation"  
✅ "추천하기"

---

## 11. UI Language Rules

---

### Default Language

- Korean (mandatory)

---

### Rules

- all user-facing text must be Korean
- no English UI unless explicitly required
- UTF-8 encoding required
- fix broken Korean text immediately

---

## 12. Animation Guidelines

---

### Use animation for:

- feedback
- reward
- transitions

---

### Avoid:

- long animations
- blocking interactions

---

## 13. Accessibility

---

### Rules

- sufficient contrast
- readable font size
- clear touch targets
- avoid overly small UI

---

## 14. Forbidden Design Patterns

---

Do NOT:

- copy Kakao UI
- use black-yellow aggressive contrast
- use chat-style bubble UI
- create overly flat corporate UI

---

## 15. AI Design Rules

---

When generating UI with AI:

- follow this design system strictly
- use honey color palette
- use Korean UI text
- maintain playful bee/flower theme
- do not generate Kakao-like UI

---

## 16. Final Principle

This design system exists to ensure:

- consistency
- identity
- usability
- scalability

---

## Final Rule

If a design decision conflicts with this document:

👉 update this document first  
👉 then apply changes