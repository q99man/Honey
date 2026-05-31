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
- Mobile auth initialization now uses a separate initializing state so stale-token session restore cannot disable the login/signup buttons while the backend is unreachable.
- Login/signup/auth service Korean UI strings touched in the auth flow were restored from mojibake to UTF-8 Korean.
- The updated dev APK was rebuilt, reinstalled on the connected Android phone after clearing the signature-mismatched old dev package, launched, and `adb reverse tcp:8080 tcp:8080` was restored.
- A follow-up login connection failure was traced to `adb reverse` disappearing when the sandboxed ADB daemon restarted; the reverse mapping was restored through the persistent host ADB session.
- Kakao map blank state was traced through device logcat to Kakao Maps native auth returning `401 Unauthorized` for dev package `com.honeytong.app.dev` with debug key hash `vD2Lb2UbcQPb2KgglHUMJd+gDvU=`.
- Touched mobile app label and Kakao initialization Korean text were restored from mojibake to UTF-8 Korean.
- Kakao native map rendering was restored on the physical phone after adding the current development key hash in Kakao Developers.
- Home map UI/UX cleanup started on the real-device path: broken Korean text in the home map was restored, Material 3 `FilterChip` usage replaced the older chip styling, and the app no longer treats a hardcoded fallback coordinate as the user's current location or calls nearby-place APIs before GPS succeeds.
- A home map widget regression test now verifies Korean controls and ensures nearby-place lookup waits for a real GPS success.
- The updated dev APK was rebuilt, installed on the connected Android phone, launched, and `adb reverse tcp:8080 tcp:8080` was restored.
- Home map visible labels, category names, login prompt copy, empty states, and map setup messages now use the shared Flutter translation resource instead of screen-local Korean literals.
- The Kakao home map fallback center is now a display-only `AppConfig` value (`HONEY_DEFAULT_MAP_LATITUDE` / `HONEY_DEFAULT_MAP_LONGITUDE`) and is not used as a current GPS location or nearby-place query input.
- Home map startup now requests the real device location automatically instead of staying on the Hongdae fallback until the location button is pressed.
- Nearby place API no longer depends on the optional generated `places.location` column, so local schemas without the Flyway spatial migration still return nearby places from latitude/longitude.
- The updated dev APK was rebuilt through `scripts/mobile-dev-usb.ps1`, installed over the existing app without uninstalling, launched on the connected Android phone, and verified with `adb reverse tcp:8080 tcp:8080` plus a real-device screenshot.
- Mobile place category display is now centralized through a shared Flutter `PlaceCategory` helper and reused by home, saved places, place detail, and place registration screens.
- Kakao home map markers now use generated Honeytong-style PNG marker assets instead of one embedded base64 marker: category-colored place pins and a distinct current-location pin were verified on the connected Android phone.
- Home map category markers now reuse the legacy web category emoji mapping (`KOREAN` rice, `CHINESE` chopsticks, `JAPANESE` sushi, `WESTERN` pasta, `SNACK` skewer, `CAFE` coffee) inside generated marker pins, and selected place markers use a larger highlighted style.
- A follow-up login connection failure was traced again to a missing physical-device `adb reverse tcp:8080 tcp:8080` mapping; the backend was healthy and the reverse mapping was restored.
- Home map first entry now starts the location permission/current-position flow after the first frame, so accepted location permissions recenter immediately and first-run denied permissions show the platform permission request instead of leaving the map on the display fallback.
- The updated dev APK was rebuilt, installed on the connected Android phone, launched, and verified with `adb reverse tcp:8080 tcp:8080` plus a real-device screenshot showing the home map centered around the current device location.
- Home map selected-marker camera behavior now follows the selected place instead of always recentering on the first place in the list; this fixes the two-place case where selecting Daedong Sikdang showed the correct card but moved the map to Viet Banh Mi.
- Closing the selected home map place card now redraws marker state without moving the map camera back to the first place, so dismissing Daedong Sikdang's card leaves the map in place.
- Home map overlay transitions now animate the selected place card in/out and move the floating actions smoothly when a card is present, reducing abrupt layout jumps around marker selection.
- Home map nearby discovery states are now separated in Korean UI: locating, loading nearby places, location-permission blocked, no nearby places, no filter results, and no search results each show distinct guidance.
- The selected home map place card now exposes a compact `상세 보기` action while ordinary list cards remain quiet with the existing chevron affordance.
- Returning from place detail after a refresh-triggering action now keeps the selected home map card attached to the freshly reloaded place object, preserving map context while updating card content.
- `scripts/mobile-dev-check.ps1` now provides a fast pre-login physical-device check for backend health and `adb reverse`, restoring the reverse mapping when it is missing without rebuilding the APK.
- Home map/list mode switching now preserves the selected place and no longer forces the map camera back to the current location when visible places or a selected place already provide exploration context. The current-location button and explicit GPS refresh still recenter to the user position.
- The updated dev APK was rebuilt through `scripts/mobile-dev-usb.ps1`, installed on the connected Android phone, launched, and verified with backend health plus `adb reverse tcp:8080 tcp:8080`.
- A dedicated web admin login route (`/admin/login`) was added, the admin shell now exposes an admin-login link, and `scripts/start-backend-local-admin.ps1` documents/automates the disabled-by-default local `SUPER_ADMIN` bootstrap flow without adding a public admin creation API.

## Active Risks

- Some older Korean text in untouched backend/frontend/mobile files may still contain mojibake. Fix touched user-facing text when encountered.
- Physical-device testing depends on stable USB debugging authorization and `adb reverse`.
- Policy values must stay server-side and configurable through `system_policies`.
- Ranking and audience-facing claims must continue to use aggregated data only.

## Next Task

Create a local `SUPER_ADMIN` account with `scripts/start-backend-local-admin.ps1`, sign in through `/admin/login`, and use `/admin/policies` to widen development registration scope before continuing the real-device map/list UX pass.

Recommended reasoning level: medium
