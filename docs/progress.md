# 📈 Development Progress

This document tracks the current development state of Honeytong.

It is used to:
- maintain development continuity
- inform Codex of current progress
- prevent duplicate or conflicting implementations
- guide next steps

---

## 1. Current Phase

Phase: PRE-DEVELOPMENT  
Status: Architecture & Design Completed

The system design, API structure, database schema, and rules are fully defined.

Development has NOT started yet.

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
- [ ] Admin policy management API pending
- [ ] Redis policy cache pending

### Place System
- [ ] Not started

### Recommendation System
- [ ] Not started

### Visit System
- [ ] Not started

### Comment System
- [ ] Not started

### Ranking System
- [ ] Not started

### Admin System
- [ ] Not started

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

Continue Policy/Admin Foundation

- add admin policy read/update APIs
- add local policy seed/import path or admin bootstrap flow

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
