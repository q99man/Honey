---
name: agent-workflow
description: Use for coding, review, and refactoring work to keep changes small, explicit, and verifiable.
---

# Agent Workflow Skill

This skill adapts the local `andrej-skills` guidance for Honeytong.

## Priority

Honeytong project rules always win:
- no hardcoded business policy values
- server-side validation
- thin controllers and service-layer business logic
- DTO responses
- separated admin and user APIs
- aggregation-based ranking
- Korean-first UTF-8 UI
- docs synced after implementation

## Rules

- State important assumptions before changing code.
- Keep the implementation scoped to the requested task.
- Do not refactor adjacent code unless the task requires it.
- Match existing project style.
- Do not delete unrelated dead code; mention it instead.
- Every changed line should trace to the task or its verification.
- Define success criteria for non-trivial work before editing.
- Prefer tests that prove the behavior over broad manual inspection.

## Verification

For backend feature work, verify with:
- focused service/controller tests when applicable
- `backend/gradlew.bat test`
- explicit formatting or diff checks when applicable
- updated `docs/progress.md`
- updated `docs/api-spec.md`, `docs/db-schema.md`, or `docs/decisions.md` when contracts, schema, or logic decisions change

## Caution

The general "avoid configurability" guideline does not apply to Honeytong business policies.
Honeytong policy values must remain admin-controlled through `system_policies` whenever they are business rules.
