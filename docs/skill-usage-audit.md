# Skill Usage Audit

This note records how Honeytong should use local skills alongside the Superpowers plugin.

## Recommendation

Use Superpowers as the primary workflow system, and keep only `skills/agent-workflow` as the Honeytong-specific supplemental checklist for coding, review, and refactoring.

The small domain skills can be treated as deprecated reference notes unless a future task explicitly needs one of their short checklists.

## Skill Assessment

| Skill | Keep Active? | Reason |
| --- | --- | --- |
| `agent-workflow` | Yes | Contains Honeytong-specific rules: policy-driven behavior, Korean-first UI, docs sync, thin controllers, DTO responses, separated admin APIs, and ranking aggregation. Superpowers does not know these repo-specific constraints by itself. |
| `admin-safe-change` | No, reference only | Its rules are already covered by `AGENTS.md`, `docs/rules.md`, `docs/api-spec.md`, and `agent-workflow`. |
| `auth-feature` | No, reference only | Its checklist overlaps with Superpowers TDD/planning plus Honey docs. Authentication work should use Superpowers workflow and `agent-workflow`. |
| `entity-api-sync` | No, reference only | Its sync rule is already in `AGENTS.md` and `agent-workflow`; use docs sync verification instead of invoking an extra skill. |
| `policy-guard` | No, reference only | Its "no hardcoded policy values" rule is already a top-level Honeytong rule and must be followed without an extra skill. |
| `ranking-batch` | No, reference only | Ranking aggregation rules are already in `docs/architecture.md`, `docs/rules.md`, and `agent-workflow`. |

## Practical Rule

For normal future development:

1. Use relevant Superpowers process skills first, such as brainstorming, TDD, systematic debugging, writing plans, or verification-before-completion.
2. Use `skills/agent-workflow` for Honeytong-specific constraints.
3. Do not additionally invoke the small domain skills unless the user explicitly asks for them or a task is unusually narrow and benefits from their checklist.

This reduces duplicated process overhead while preserving the project-specific guardrails that matter.
