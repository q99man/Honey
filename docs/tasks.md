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
- [x] Configure Redis connection
- [x] Add global exception handler
- [x] Add base response format (success/error)
- [x] Configure logging
- [x] Add Windows tool session bootstrap scripts for Git, Node/npm, Java, and Gradle

---

## 1. Authentication System

### Core Auth
- [x] Implement signup API
- [x] Implement login API
- [x] Implement OAuth login (Kakao, Naver, Google)
- [x] Implement JWT authentication (access + refresh)
- [x] Implement logout
- [x] Implement token refresh
- [x] Connect frontend login/signup/logout token flow

### Phone Verification
- [x] Send verification code API
- [x] Verify code API
- [x] Store verification state
- [x] Enforce phone verification in core actions
- [x] Connect frontend phone verification flow
- [x] Add production SMS provider adapter
- [x] Add SOLAPI SMS provider adapter
- [x] Add live SMS delivery smoke script
- [x] Verify live SMS delivery with provider credentials

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
- [x] Connect frontend region verification flow

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
- [x] Implement update place API
- [x] Implement delete place API
- [x] Connect frontend place registration flow
- [x] Connect frontend my registered places read flow
- [x] Connect frontend place owner edit/delete flow

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
- [x] Connect place detail frontend recommendation action

---

## 6. Visit System (Critical)

- [x] Create visit entity
- [x] Implement GPS validation logic
- [x] Implement visit API
- [x] Enforce visit cooldown
- [x] Store visit validity
- [x] Update place_stats on visit
- [x] Update user trust/level on visit
- [x] Implement visit policy API
- [x] Implement my visits API
- [x] Implement place visit summary API
- [x] Connect place detail frontend visit verification action

---

## 7. Comment System

- [x] Create comment entity
- [x] Enforce one comment per user per place
- [x] Implement create comment API
- [x] Implement update comment API
- [x] Implement delete comment API
- [x] Implement place comment list API
- [x] Implement user comment list API
- [x] Connect place detail frontend comment actions

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
- [x] Implement disabled-by-default ranking scheduler foundation
- [x] Implement controlled ranking history finalization foundation
- [x] Implement place ranking history read API
- [x] Connect ranking frontend read views

---

## 9. Trust & Level System

- [x] Implement valid-visit trust score update logic
- [x] Implement trust grade evaluation
- [x] Implement recommendation weight calculation
- [x] Implement visit EXP gain logic
- [x] Implement level up logic
- [x] Implement level history tracking

---

## 10. Policy System (Very Important)

- [x] Create system_policies table
- [x] Implement policy loading service
- [x] Add opt-in policy seed/bootstrap import path
- [x] Implement admin policy read/update API
- [x] Implement policy cache (Redis)
- [x] Connect admin policy frontend management flow
- [ ] Replace hardcoded values with policy values

### Policies to Support
- [x] daily recommendation limit
- [x] visit radius
- [x] visit cooldown
- [x] ranking weights
- [x] visit growth/trust signal values
- [x] trust weights
- [x] level thresholds
- [x] region change cooldown
- [x] place registration limit

---

## 11. Report System

- [x] Create report entity
- [x] Implement create report API
- [x] Implement user report list API
- [x] Connect report frontend user flow
- [x] Integrate report with admin workflow

---

## 12. Admin System (Critical)

### Admin Base
- [x] Implement admin authentication/authorization
- [x] Separate admin API routes
- [x] Add disabled-by-default local admin bootstrap/test account flow

### Dashboard
- [x] Implement admin dashboard metrics
- [x] Connect admin dashboard frontend read flow
- [x] Connect admin policy frontend management flow

### User Management
- [x] Implement admin user list
- [x] Implement user detail view
- [x] Implement user sanction creation API
- [x] Enforce active user sanctions on core write actions
- [x] Implement trust adjustment
- [x] Implement recommendation weight adjustment
- [x] Connect admin user management frontend flow

### Place Management
- [x] Implement admin place list/detail read APIs
- [x] Implement place moderation status control foundation
- [x] Implement approval status control
- [x] Implement exposure control
- [x] Implement franchise review status control
- [x] Implement place score adjustment foundation
- [x] Connect admin place management frontend flow

### Report Management
- [x] Implement report list
- [x] Implement report processing
- [x] Apply actions (hide, delete, sanction)
- [x] Connect admin report management frontend flow

### Comment Management
- [x] Implement admin comment list API
- [x] Implement comment blind control
- [x] Implement comment delete control
- [x] Connect admin comment moderation frontend flow

### Recommendation and Visit Management
- [x] Implement recommendation log read API
- [x] Implement visit log read API
- [x] Implement recommendation invalidation
- [x] Implement visit invalidation
- [x] Connect admin recommendation and visit moderation frontend flow

---

## 13. Ranking Admin Tools

- [x] Implement season creation
- [x] Implement season status change
- [x] Implement ranking recalculation trigger
- [x] Implement ranking exclusion control
- [x] Implement ranking history finalization trigger

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

- [x] Implement user action logs
- [x] Implement admin user action log read API
- [x] Implement admin action log entity and policy update logging
- [x] Implement admin action log read API
- [x] Log recommendation / visit / report / admin actions

---

## 17. Performance Optimization

- [ ] Optimize place_stats updates
- [ ] Add caching for hot endpoints
- [x] Optimize ranking queries
- [x] Add ranking query indexes
- [ ] Add indexes for heavy queries

---

## 18. Testing

- [ ] Unit test for core services
- [x] Test recommendation rules
- [x] Test visit validation
- [x] Test policy-based logic
- [x] Test duplication prevention
- [x] Test ranking logic

---

## 19. Deployment Preparation

- [x] Prepare production profile
- [x] Configure environment variables
- [x] Setup database migration strategy
- [x] Setup logging for production
- [x] Setup monitoring basics
- [x] Add environment-driven ranking scheduler settings
- [x] Add MVP release candidate and deployment checklist
- [x] Run production-profile startup rehearsal with Flyway and schema validation
- [x] Run staging-like seed/bootstrap and core API smoke rehearsal
- [x] Run frontend browser smoke against staging-like backend
- [x] Add one-command local staging smoke wrapper
- [x] Add MVP release runbook
- [x] Prepare release branch packaging

---

## 20. UI and Localization
- [x] Add UI language rules document
- [x] Add UTF-8 editor config
- [x] Add git attributes for UTF-8
- [x] Add Korean encoding check script
- [x] Add Korean locale file as default UI source
- [x] Restore broken Korean UI text in touched frontend screens and locale file
- [x] Add shared frontend authenticated API client behavior for core actions
- [x] Add My Page auth and phone verification UI
- [x] Add place registration UI connected to backend policy and creation APIs
- [x] Add user report creation and my report list UI
- [x] Add admin report management UI
- [x] Add admin dashboard read UI
- [x] Add admin policy management UI
- [x] Add admin user management UI
- [x] Add admin place management UI
- [x] Add admin activity moderation UI
- [x] Add admin audit log read UI

---

## 21. MVP Completion Criteria

MVP is complete when:

- [x] user can sign up and login
- [x] phone verification works
- [x] region verification works
- [x] user can register place
- [x] user can recommend place
- [x] user can verify visit
- [x] user can comment
- [x] ranking works per region
- [x] admin can manage reports
- [x] admin can adjust policies
- [x] system prevents basic abuse

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
