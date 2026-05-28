# Technical Decisions

This file keeps only active decisions that should guide future work. Historical implementation logs were removed to keep project context compact.

## 1. Product Architecture

- Honeytong uses a Spring Boot monolith for the MVP.
- Code is organized by domain: auth, user, region, place, recommendation, visit, comment, ranking, admin, policy, report, notification, analytics, and common infrastructure.
- Controllers stay thin. Business rules live in services. Repositories handle persistence only.

## 2. Policy-Driven Rules

- Business policy values must not be hardcoded.
- Limits, cooldowns, weights, ranking values, trust values, registration scope, and similar settings come from `system_policies`.
- Missing or invalid policy values should fail clearly rather than silently falling back to embedded business defaults.

## 3. Authentication And Trust

- Authentication uses JWT access and refresh tokens.
- Phone verification is required for core write actions.
- Trust and level are separate systems: level rewards activity, trust controls influence and reliability.
- Active sanctions must block protected user actions server-side.

## 4. Region And Place Integrity

- Users have one primary verified dong.
- Region verification is GPS-based.
- Place create/update with a road or jibun address resolves coordinates through Kakao Local on the backend.
- Address-derived coordinates and administrative dong take precedence over client-provided coordinates/dong when an address is supplied.

## 5. Ranking And Aggregation

- Public ranking reads use aggregated ranking tables and place stats.
- Ranking is not calculated ad hoc from raw activity rows in request handlers.
- Visit signals have higher ranking importance than recommendations, and recommendations have higher importance than comments.
- Stars are derived from seasonal regional ranking results.
- Ranking exclusion is an admin-controlled place state and should not change normal place exposure by itself.

## 6. Images

- Image binary data is not stored in MySQL.
- The MVP upload contract is `POST /api/uploads/images` for `PLACE`, `PROFILE`, and `VISIT`.
- The API returns public image URLs; relational tables store URLs only.
- Local MVP storage can later be replaced by object storage without changing the client contract.

## 7. Redis And Caching

- Redis is optional in local development.
- DB rows remain authoritative.
- Redis may cache policies, ranking reads, phone verification state, recommendation daily counters, visit cooldowns, analytics, and locks only through explicit service boundaries.
- Redis health checks stay disabled unless Redis is a required runtime dependency.

## 8. Async And Performance

- Heavy recalculations such as demographic/audience stats should run asynchronously after the main transaction commits when correctness allows it.
- Expensive list, audit, sanction, demographic, search, and nearby queries should use purpose-built indexes.
- Nearby place search uses DB-side spatial querying with MySQL spatial functions and generated location columns.
- Korean place search uses FULLTEXT ngram for multi-character search with a one-character fallback.

## 9. Localization

- Korean is the default UI language.
- Frontend/mobile localization should stay lightweight and dependency-conscious.
- Do not replace Korean UI with English during cleanup or refactoring.

## 10. Mobile Development

- Physical Android dev testing should prefer USB mode with `adb reverse`.
- `scripts/mobile-dev-usb.ps1` builds the dev APK with `HONEY_DEV_API_BASE_URL=http://127.0.0.1:8080`, installs it, launches it, and restores reverse forwarding.
- `scripts/mobile-dev-lan.ps1` is a secondary option for networks where the phone can reach the PC directly.
- If Android install reports signature mismatch, uninstall `com.honeytong.app.dev` and reinstall the dev APK.

## 11. Repository Hygiene

- Keep project memory compact.
- Keep core direction/contract docs; remove one-off plans, handoffs, smoke result files, local skill packs, and obsolete hooks.
- Add new docs only when they will remain useful after the current task.
