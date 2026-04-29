# Honeytong Repository Instructions

## Read these files first
Before doing any work, read these files in `/docs`:

- prd.md
- rules.md
- db-schema.md
- api-spec.md
- architecture.md
- tasks.md
- progress.md
- decisions.md

## Project identity
Honeytong is a mobile-first, location-based local restaurant discovery platform.

Core priorities:
- exploration-first UX
- trust-based participation
- region-aware rules
- admin-controlled policies
- aggregation-based ranking

## Non-negotiable rules

- Do not hardcode business policy values
- All validation must be server-side
- Controllers must be thin
- Business logic must be in services
- Use DTOs for all responses
- Separate admin APIs from user APIs
- Ranking must use aggregated data

## UI language and encoding rules

- All user-facing UI text must be Korean by default
- Do not replace Korean UI with English unless explicitly requested
- All text files must be saved in UTF-8
- If a file contains broken Korean text, fix encoding before continuing
- Prefer locale/resource files for UI text instead of scattering hardcoded strings
- Before finishing UI work, verify that Korean text is displayed correctly
- Admin UI is also Korean-first by default

## Workflow rules

- Check tasks.md before coding
- Update progress.md after work
- Update decisions.md if logic changes
- Sync db-schema.md and api-spec.md if changed

## Implementation priority

1. auth
2. user
3. region
4. place
5. recommendation
6. visit
7. comment
8. ranking
9. admin

## Verification checklist

Before finishing:

- formatting passed
- tests passed
- no hardcoded policies
- docs synced

## Skills

Use skills in `/skills` when relevant.
Prefer using an existing skill over creating new logic.