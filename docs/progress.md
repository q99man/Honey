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
- [ ] Backend Spring Boot project not started

### Authentication
- [ ] Not started

### Phone Verification
- [ ] Not started

### User System
- [ ] Not started

### Region System
- [ ] Not started

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

### Policy System
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

👉 Start implementing Authentication System

- signup
- login
- JWT
- OAuth (optional early or later)

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
