# 🧾 Development Tasks

This document defines the implementation roadmap for Honeytong.

The goal is:
- start from MVP
- ensure stable core system
- expand gradually without breaking architecture

---

## 0. Project Setup

- [x] Initialize Spring Boot project
- [x] Configure Gradle dependencies
- [x] Set up package structure (domain-based)
- [x] Configure application.yml (dev / prod)
- [x] Configure database connection (MySQL)
- [ ] Configure Redis connection
- [x] Add global exception handler
- [x] Add base response format (success/error)
- [ ] Configure logging

---

## 1. Authentication System

### Core Auth
- [x] Implement signup API
- [x] Implement login API
- [ ] Implement OAuth login (Kakao, Naver, Google)
- [x] Implement JWT authentication (access + refresh)
- [x] Implement logout
- [x] Implement token refresh

### Phone Verification
- [x] Send verification code API
- [x] Verify code API
- [x] Store verification state
- [ ] Enforce phone verification in core actions

---

## 2. User System

- [x] Create user entity
- [x] Create user_auth entity
- [x] Create user_trust entity
- [x] Create user_level entity
- [x] Implement get my profile API
- [x] Implement update profile API
- [x] Implement get user status API
- [x] Implement user activity summary

---

## 3. Region System

- [x] Create region_city / district / dong tables
- [x] Add region seed CSV import path
- [x] Add full Korean administrative dong seed data
- [x] Implement region lookup APIs
- [x] Implement GPS-based region verification
- [x] Verify GPS-based region flow with live Kakao smoke test
- [x] Implement primary region assignment
- [x] Implement region change API
- [x] Implement region change policy validation

---

## 4. Place System (Core Domain)

### Basic Structure
- [x] Create place entity
- [x] Create place_images entity
- [x] Create place_stats entity

### Features
- [x] Implement place creation API
- [x] Validate place registration policy
- [x] Implement place detail API
- [x] Implement nearby places API
- [x] Implement region-based place list
- [x] Implement place search API
- [ ] Implement update place API
- [ ] Implement delete place API

---

## 5. Recommendation System

- [x] Create recommendation entity
- [x] Enforce unique (user, place)
- [x] Implement recommend API
- [x] Implement cancel recommendation API
- [x] Validate daily recommendation limit
- [x] Apply trust-based weight
- [x] Update place_stats on recommendation
- [x] Implement recommendation policy API

---

## 6. Visit System (Critical)

- [x] Create visit entity
- [x] Implement GPS validation logic
- [x] Implement visit API
- [x] Enforce visit cooldown
- [x] Store visit validity
- [x] Update place_stats on visit
- [ ] Update user trust/level on visit
- [x] Implement visit policy API
- [x] Implement my visits API
- [x] Implement place visit summary API

---

## 7. Comment System

- [x] Create comment entity
- [x] Enforce one comment per user per place
- [x] Implement create comment API
- [x] Implement update comment API
- [x] Implement delete comment API
- [x] Implement place comment list API
- [x] Implement user comment list API

---

## 8. Ranking System

### Structure
- [x] Create seasons table
- [x] Create place_season_scores table
- [x] Create ranking history table

### Logic
- [x] Implement score calculation service
- [x] Implement ranking aggregation logic
- [x] Implement ranking read API (dong/district/city)
- [x] Implement season handling

---

## 9. Trust & Level System

- [ ] Implement trust score update logic
- [ ] Implement trust grade evaluation
- [ ] Implement recommendation weight calculation
- [ ] Implement level exp gain logic
- [ ] Implement level up logic
- [ ] Implement level history tracking

---

## 10. Policy System (Very Important)

- [x] Create system_policies table
- [x] Implement policy loading service
- [x] Add opt-in policy seed/bootstrap import path
- [x] Implement admin policy read/update API
- [ ] Implement policy cache (Redis)
- [ ] Replace hardcoded values with policy values

### Policies to Support
- [x] daily recommendation limit
- [x] visit radius
- [x] visit cooldown
- [x] ranking weights
- [ ] trust weights
- [x] region change cooldown
- [x] place registration limit

---

## 11. Report System

- [ ] Create report entity
- [ ] Implement create report API
- [ ] Implement user report list API
- [ ] Integrate report with admin workflow

---

## 12. Admin System (Critical)

### Admin Base
- [x] Implement admin authentication/authorization
- [x] Separate admin API routes
- [x] Add disabled-by-default local admin bootstrap/test account flow

### Dashboard
- [ ] Implement admin dashboard metrics

### User Management
- [ ] Implement admin user list
- [ ] Implement user detail view
- [ ] Implement user sanction system
- [ ] Implement trust adjustment

### Place Management
- [ ] Implement place moderation
- [ ] Implement approval status control
- [ ] Implement exposure control
- [ ] Implement franchise review

### Report Management
- [ ] Implement report list
- [ ] Implement report processing
- [ ] Apply actions (hide, delete, sanction)

---

## 13. Ranking Admin Tools

- [x] Implement season creation
- [x] Implement season status change
- [x] Implement ranking recalculation trigger
- [ ] Implement ranking exclusion control

---

## 14. Audience Tag System (Optional for MVP)

- [ ] Create audience stats table
- [ ] Aggregate demographic data
- [ ] Generate audience tags
- [ ] Expose tags via API

---

## 15. Mission System (Optional for MVP)

- [ ] Create mission table
- [ ] Create user mission progress table
- [ ] Implement mission tracking
- [ ] Implement reward claim logic

---

## 16. Logging System

- [ ] Implement user action logs
- [x] Implement admin action log entity and policy update logging
- [ ] Log recommendation / visit / report / admin actions

---

## 17. Performance Optimization

- [ ] Optimize place_stats updates
- [ ] Add caching for hot endpoints
- [ ] Optimize ranking queries
- [ ] Add indexes for heavy queries

---

## 18. Testing

- [ ] Unit test for core services
- [ ] Test recommendation rules
- [ ] Test visit validation
- [ ] Test policy-based logic
- [ ] Test duplication prevention
- [ ] Test ranking logic

---

## 19. Deployment Preparation

- [ ] Prepare production profile
- [ ] Configure environment variables
- [ ] Setup database migration strategy
- [ ] Setup logging for production
- [ ] Setup monitoring basics

---

## 20. UI and Localization
- [x] Add UI language rules document
- [x] Add UTF-8 editor config
- [x] Add git attributes for UTF-8
- [x] Add Korean encoding check script
- [x] Add Korean locale file as default UI source

---

## 21. MVP Completion Criteria

MVP is complete when:

- [ ] user can sign up and login
- [ ] phone verification works
- [ ] region verification works
- [ ] user can register place
- [ ] user can recommend place
- [ ] user can verify visit
- [ ] user can comment
- [ ] ranking works per region
- [ ] admin can manage reports
- [ ] admin can adjust policies
- [ ] system prevents basic abuse

---

## 22. Future Expansion Tasks

- [ ] notification system
- [ ] advanced analytics
- [ ] fraud detection system
- [ ] recommendation optimization
- [ ] multi-language UI
- [ ] search optimization
- [ ] AI-based tagging (optional)

---

## 23. Task Execution Rule

When implementing:

1. Follow agent.md instructions
2. Apply rules.md constraints
3. Use db-schema.md structure
4. Implement api-spec.md endpoints
5. Update progress.md after completion

Never skip validation or policy checks.
