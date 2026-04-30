# 📈 Development Progress

This document tracks the current development state of Honeytong.

It is used to:
- maintain development continuity
- inform Codex of current progress
- prevent duplicate or conflicting implementations
- guide next steps

---

## 1. Current Phase

Phase: BACKEND MVP DEVELOPMENT
Status: Core auth, user, region, policy, admin policy, and place foundation in progress

The system design, API structure, database schema, and rules are defined.
Backend MVP implementation is progressing in small API increments.

---

## 2. Completed Documents

The following documents are finalized:

- [x] prd.md
- [x] rules.md
- [x] agent.md
- [x] db-schema.md
- [x] api-spec.md
- [x] architecture.md
- [x] tasks.md

All documents are consistent with each other.

---

## 3. Implementation Status

### Harness / Repository Setup
- [x] Root Git repository initialized
- [x] Root GitHub remote configured
- [x] UTF-8 editor and Git attributes configured
- [x] Frontend lint/build checks pass
- [x] CI updated for current frontend-first state and future backend Gradle project
- [x] Agent harness now requires next recommended task and matching reasoning level after work
- [x] Backend Spring Boot project initialized
- [x] Backend Gradle Wrapper generated and verified
- [x] Backend default DB schema set to `honey`

### Authentication
- [x] Local signup API implemented
- [x] Local login API implemented
- [x] Access token / refresh token structure implemented
- [x] Refresh token rotation implemented
- [x] Logout revokes refresh token
- [x] Phone verification code send API implemented
- [x] Phone verification code verify API implemented
- [x] Phone verification status API implemented
- [x] Phone verification guard annotation/aspect implemented for future core actions
- [x] Phone verification unit tests added
- [ ] OAuth not implemented yet
- [ ] Real SMS provider not connected yet

### Phone Verification
- [x] DB-backed verification code state implemented
- [x] Development sender logs verification code
- [x] Server-side phone verification guard mechanism implemented
- [ ] Production SMS provider integration pending
- [ ] Phone verification guard for future core actions pending

### User System
- [x] User profile API implemented
- [x] User profile update API implemented
- [x] User status API implemented
- [x] User growth API implemented
- [x] User activity summary API implemented with replaceable reader
- [x] User service unit tests added
- [ ] Activity summary still returns zero until place/recommendation/visit/comment domains are implemented

### Region System
- [x] Region city/district/dong entities and repositories implemented
- [x] Region lookup APIs implemented
- [x] My primary region API implemented
- [x] GPS verification service boundary implemented
- [x] Kakao Map/Local API environment variable placeholders prepared
- [x] Kakao coordinate-to-region resolver implemented
- [x] Region seed CSV import path implemented
- [x] Region seed sample CSV added for Kakao administrative dong code format
- [x] Full Korean administrative dong seed CSV generated from `KIKcd_H.20260325.xlsx`
- [x] Region seed conversion script added
- [x] Region seed imported into local MySQL schema `honey`
- [x] Live Kakao GPS region verification smoke test passed for Seoul Mapo-gu Seogyo-dong
- [x] Region change API implemented
- [x] Region change policy API implemented
- [x] Region change cooldown reads `system_policies.region.change_cooldown_day`
- [x] Region service unit tests added
- [x] Kakao region resolver unit test added
- [x] Production seed file aligns `region_dong.code` with Korean administrative dong codes used by Kakao

### Policy System
- [x] `system_policies` entity and repository implemented
- [x] Required integer policy loader implemented
- [x] Region change cooldown wired to policy service
- [x] Required string policy loader implemented
- [x] Admin policy list/update API implemented at `/api/admin/policies`
- [x] Admin region policy API implemented at `/api/admin/policies/region`
- [x] Opt-in policy seed import path added with `POLICY_SEED_ENABLED`
- [ ] Redis policy cache pending

### Place System
- [x] `places`, `place_images`, and `place_stats` entities/repositories implemented
- [x] Place creation API implemented at `POST /api/places`
- [x] Place creation requires phone verification via `@RequirePhoneVerified`
- [x] Place creation validates primary region and registration scope policy
- [x] Place registration limit reads `system_policies.place.registration_limit`
- [x] Place registration scope reads `system_policies.region.registration_scope`
- [x] Place detail API implemented at `GET /api/places/{placeId}`
- [x] Region-filtered place list API implemented at `GET /api/places`
- [x] Nearby place list API implemented at `GET /api/places/nearby`
- [x] Place search API implemented at `GET /api/places/search`
- [x] Place registration policy API implemented at `GET /api/places/registration-policy`
- [x] Frontend mock place list removed and connected to real `/api/places`
- [ ] Update/delete APIs pending

### Recommendation System
- [x] `recommendations` entity and repository implemented
- [x] Recommend API implemented at `POST /api/places/{placeId}/recommend`
- [x] Cancel recommendation API implemented at `DELETE /api/places/{placeId}/recommend`
- [x] Recommendation policy API implemented at `GET /api/places/{placeId}/recommend-policy`
- [x] My recommendations API implemented at `GET /api/users/me/recommendations`
- [x] Recommendation requires phone verification via `@RequirePhoneVerified`
- [x] One active recommendation per user/place is enforced
- [x] Daily recommendation limit reads `system_policies.recommend.daily_limit`
- [x] User trust recommendation weight is applied
- [x] `place_stats.recommend_count`, `score_total`, and `trust_weighted_score` update on recommend/cancel

### Visit System
- [x] `visits` entity/repository implemented
- [x] Visit verification API implemented at `POST /api/places/{placeId}/visits`
- [x] Visit policy API implemented at `GET /api/places/{placeId}/visit-policy`
- [x] My visits API implemented at `GET /api/users/me/visits`
- [x] Place visit summary API implemented at `GET /api/places/{placeId}/visits/summary`
- [x] Visit verification requires phone verification via `@RequirePhoneVerified`
- [x] GPS distance validation reads `system_policies.visit.radius_meter`
- [x] Visit cooldown reads `system_policies.visit.cooldown_hour`
- [x] Valid visits update `place_stats.visit_count`, `score_total`, and `trust_weighted_score`
- [ ] User trust/level update on visit pending

### Comment System
- [x] `comments` entity/repository implemented
- [x] Create comment API implemented at `POST /api/places/{placeId}/comments`
- [x] Update comment API implemented at `PATCH /api/comments/{commentId}`
- [x] Delete comment API implemented at `DELETE /api/comments/{commentId}`
- [x] Place comment list API implemented at `GET /api/places/{placeId}/comments`
- [x] My comments API implemented at `GET /api/users/me/comments`
- [x] Comment create/update requires phone verification via `@RequirePhoneVerified`
- [x] One visible comment per user/place is enforced
- [x] Deleted comments can be restored by writing again without violating the DB unique key
- [x] Valid visible comments update `place_stats.comment_count`, `score_total`, and `trust_weighted_score`

### Ranking System
- [x] `seasons`, `place_season_scores`, and `place_ranking_history` entities/repositories implemented
- [x] Place ranking read API implemented at `GET /api/rankings/places`
- [x] Current season API implemented at `GET /api/rankings/seasons/current`
- [x] Ranking reads use `place_season_scores` aggregate rows instead of raw recommendation/visit/comment rows
- [x] Ranking read supports dong/district/city region types and optional `seasonCode`
- [x] Admin season list/create/update APIs implemented at `/api/admin/seasons`
- [x] Admin ranking recalculation API implemented at `POST /api/admin/rankings/recalculate`
- [x] Ranking aggregation writes `place_season_scores` from `place_stats`
- [x] Ranking aggregation reads `ranking.recommend_weight`, `ranking.visit_weight`, and `ranking.comment_weight`
- [ ] Automated scheduler for ranking aggregation pending
- [ ] Ranking exclusion control pending

### Admin System
- [x] Admin API route protection added for `/api/admin/**`
- [x] Policy updates restricted to `SUPER_ADMIN`
- [x] Admin policy changes are logged in `admin_action_logs`
- [x] Disabled-by-default local admin bootstrap/test account flow added
- [ ] Dashboard/report/user/place admin tools pending

---

## 4. Key Decisions (Summary)

- System is mobile-first
- Backend is Spring Boot monolith
- Database is MySQL
- Redis is used for caching and cooldown
- Ranking is aggregation-based (not real-time)
- Policy values must NOT be hardcoded
- Admin must be able to control all key parameters

---

## 5. MVP Scope (Confirmed)

The MVP must include:

- user signup/login
- phone verification
- region verification (GPS-based)
- place registration (limited)
- recommendation system
- visit verification (GPS-based)
- comment system
- regional ranking (dong/district/city)
- report system
- admin dashboard
- admin report handling
- admin policy control

---

## 6. Development Priority

Next implementation order:

1. Authentication system
2. Phone verification
3. User system
4. Region system
5. Place system
6. Recommendation system
7. Visit system
8. Comment system
9. Ranking system
10. Policy system
11. Admin system

---

## 7. Current Next Step

Next task:

Start Report System Foundation

- create report entity and user report APIs
- keep report handling ready for admin moderation workflow
- report targets should support place, comment, and user without applying automated sanctions yet

---

## 8. Known Risks

- GPS spoofing (visit validation risk)
- duplicate account creation (phone verification required)
- ranking manipulation attempts
- policy misconfiguration
- early-stage data imbalance

Mitigation strategy:
- strong validation
- admin monitoring tools
- policy control

---

## 9. Notes for Codex

- Follow agent.md strictly
- Never hardcode policy values
- Always validate duplication and cooldown
- Keep controllers thin
- Implement logic in services
- Use DB schema as defined
- Do not change API structure without reason

---

## 10. Update Rules

This file MUST be updated when:

- a feature is completed
- a major decision changes
- a new system is added
- priorities shift

---

## 11. Example Update Format

When updating progress:

### Example:

Authentication:
- [x] signup API implemented
- [x] login API implemented
- [ ] OAuth not implemented yet

Region System:
- [x] region tables created
- [x] region lookup API implemented
- [ ] GPS verification pending

---

## 12. Final Note

This document is critical for maintaining development flow.

Always keep it updated.

It ensures:
- consistent progress
- reduced confusion
- better Codex performance
