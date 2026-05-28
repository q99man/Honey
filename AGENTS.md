# Honeytong Repository Instructions

## Project Identity

Honeytong is a mobile-first, location-based local restaurant discovery platform.

Core priorities:
- exploration-first UX
- trust-based participation
- region-aware rules
- admin-controlled policies
- aggregation-based ranking

## Read Only What You Need

Always check:
- `docs/tasks.md`
- `docs/progress.md`

For product, policy, architecture, or domain behavior, also check:
- `docs/prd.md`
- `docs/rules.md`
- `docs/architecture.md`
- `docs/decisions.md`

For API or database changes, also check:
- `docs/api-spec.md`
- `docs/db-schema.md`

## Non-Negotiable Rules

- Do not hardcode business policy values.
- All validation must be server-side.
- Controllers must be thin.
- Business logic must be in services.
- Use DTOs for all responses.
- Separate admin APIs from user APIs.
- Ranking must use aggregated data.

## UI Language And Encoding

- User-facing UI text must be Korean by default.
- Admin UI is also Korean-first by default.
- Do not replace Korean UI with English unless explicitly requested.
- Save text files as UTF-8.
- If touched UI text contains broken Korean, fix it before finishing.
- Prefer locale/resource files for reusable UI text.

## Workflow

- Keep changes scoped to the requested task.
- On Windows, normalize local commands with `scripts/dev-env.ps1` or use the project PowerShell wrappers.
- Update `docs/progress.md` after meaningful work.
- Update `docs/decisions.md` only when behavior, architecture, or operating policy changes.
- Sync `docs/db-schema.md` and `docs/api-spec.md` only when schema or API contracts change.
- After finishing, include the next recommended task and reasoning level.

## Reasoning Level Guide

- low: documentation, formatting, copy, isolated UI text
- medium: ordinary feature work, API wiring, frontend state, focused refactoring
- high: auth, authorization, policy validation, ranking, data model, security, cross-domain logic
- xhigh: major architecture changes, complex migrations, large refactors, production incidents

## Implementation Priority

1. auth
2. user
3. region
4. place
5. recommendation
6. visit
7. comment
8. ranking
9. admin

## Verification Checklist

Before finishing, report what was actually verified:
- formatting/static checks when applicable
- tests/builds when applicable
- docs synced when applicable
- any verification that could not be run
