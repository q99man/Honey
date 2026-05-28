# Honeytong

Honeytong is a mobile-first, location-based local restaurant discovery platform.

The project is currently focused on Flutter mobile stabilization, real-device QA, backend policy correctness, and keeping development workflow lightweight.

## Key Docs

- `AGENTS.md`: working rules for Codex and contributors
- `docs/tasks.md`: roadmap and implementation order
- `docs/progress.md`: compact current status and next task
- `docs/prd.md`: product direction
- `docs/rules.md`: business rules
- `docs/architecture.md`: system structure
- `docs/decisions.md`: active technical decisions
- `docs/api-spec.md`: API contract, when API work needs it
- `docs/db-schema.md`: schema contract, when DB work needs it

## Local Commands

Normalize Windows tool paths:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev-env.ps1
```

Run backend Gradle:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 test
```

Run frontend npm:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend-npm.ps1 run build
```

Run mobile dev app on a USB-connected Android phone:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-usb.ps1
```

Check only phone/backend USB forwarding:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mobile-dev-check.ps1
```
