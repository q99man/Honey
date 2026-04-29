# UI Language Rules

This document defines language and encoding rules for all user-facing UI in Honeytong.

---

## 1. Default UI Language

The default user-facing language is Korean.

Rules:
- All visible UI text must be written in Korean by default
- Do not replace Korean UI text with English unless explicitly requested
- Internal code, variable names, package names, class names, and technical docs may remain in English

Examples of user-facing text:
- button labels
- page titles
- section headers
- placeholders
- validation messages
- dialogs
- toasts
- onboarding text
- empty state text
- user-facing error messages
- tab names
- filter labels
- admin screen labels shown to Korean operators

---

## 2. English Usage Policy

English may be used only when:
- it is part of a technical identifier
- it is required for implementation
- multilingual support is intentionally implemented
- an external API or library requires English naming

Do not use English as the default visible UI language.

Wrong:
- "Confirm"
- "Cancel"
- "Nearby Places"

Correct:
- "확인"
- "취소"
- "내 주변 꽃"

---

## 3. Multilingual Structure

The product may support:
- Korean
- English
- Japanese

However:
- Korean must be the default locale
- Do not build screens with English-first text and translate later
- If i18n is introduced, Korean locale resources must be complete first
- English and Japanese should follow Korean source text, not replace it

---

## 4. Encoding Rules

All text files must use UTF-8 encoding.

Requirements:
- source files must be saved as UTF-8
- markdown files must be saved as UTF-8
- locale files must be saved as UTF-8
- JSON locale resources must preserve Korean text correctly
- properties, yaml, and sql files must also be UTF-8 safe when they contain Korean

Never introduce mojibake or corrupted Korean strings.

Examples of broken text:
- ����
- ì•ˆë…•
- Ã¬
- replacement characters like �

If Korean text is broken:
- stop and fix encoding first
- do not continue with corrupted strings

---

## 5. Implementation Rules

When creating UI:
- prefer locale/resource files over scattered hardcoded text
- keep Korean strings natural and readable
- do not transliterate Korean into English unless explicitly asked
- do not silently replace Korean labels with English
- avoid mixing Korean and English in the same UI sentence unless product-required

Recommended:
- use `ko.json` as the primary UI text source
- keep text keys stable and descriptive
- review Korean wording before finishing UI work

---

## 6. Validation Rule

Before completing UI-related work:
- verify user-facing text is Korean
- verify files are UTF-8
- verify Korean text is not corrupted
- verify no accidental English UI replaced Korean labels
- verify locale files and rendered UI match

---

## 7. Admin UI Rule

Admin UI is also Korean-first unless explicitly requested otherwise.

Reason:
- product owner and operators are Korean-speaking
- admin usability matters
- operational mistakes increase when mixed-language UI is inconsistent

---

## 8. Final Rule

User-facing UI must remain Korean-first and UTF-8 safe unless explicitly changed by product decision.