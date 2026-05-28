# Development Progress

## Current Phase

Phase: Flutter real-device stabilization and workflow simplification.

The backend MVP is broadly implemented. Current work is focused on making the mobile app reliable on real Android devices, keeping Korean UI text correct, and reducing repository/process overhead so Codex can work from compact project memory.

## Current Status

- Backend auth, user, region, place, recommendation, visit, comment, ranking, admin, policy, report, notification, analytics, fraud, search, and image upload foundations are implemented.
- Backend health is available at `GET /actuator/health`.
- Real image upload is available through `POST /api/uploads/images` and mobile place/profile image flows use it.
- Flutter dev/prod flavors exist.
- Real-device Android development should use `scripts/mobile-dev-usb.ps1`, which builds the dev APK with `http://127.0.0.1:8080` and restores `adb reverse tcp:8080 tcp:8080`.
- LAN mobile development is secondary and available through `scripts/mobile-dev-lan.ps1`.
- The old local skills, git hooks, release handoff docs, one-off smoke docs, and large UI prompt packs have been removed to keep the workspace compact.

## Recent Notes

- Fullscreen place image viewer was verified on a physical Android phone for tap, close, drag between images, and pinch zoom.
- Login/signup failures on the phone were traced to an old installed dev APK with a different signature. The stale package was uninstalled, the latest dev APK was installed, and USB reverse was restored.
- The repository cleanup removed duplicated workflow guidance. Future work should rely on `AGENTS.md`, the core docs, and current Codex/Superpowers capabilities instead of local skill packs.
- Mobile login/signup now preserve backend error messages and show Korean connection guidance when the phone cannot reach the dev backend.
- The updated dev APK was rebuilt, installed on the connected Android phone, launched, and `adb reverse tcp:8080 tcp:8080` was restored.
- Real-device login/signup smoke passed on the connected phone.
- `mobile/lib` Korean mojibake scan found no remaining common broken-text patterns, and the full Flutter mobile test suite passed.

## Active Risks

- Some older Korean text in untouched backend/frontend/mobile files may still contain mojibake. Fix touched user-facing text when encountered.
- Physical-device testing depends on stable USB debugging authorization and `adb reverse`.
- Policy values must stay server-side and configurable through `system_policies`.
- Ranking and audience-facing claims must continue to use aggregated data only.

## Next Task

Create a clean checkpoint for the current workflow cleanup, mobile dev scripts, auth error handling, and verification updates so the large working-tree change set is easier to review.

Recommended reasoning level: low
