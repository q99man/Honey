# 🧾 Development Tasks

This document defines the implementation roadmap for Honeytong.

The goal is:
- start from MVP
- ensure stable core system
- expand gradually without breaking architecture

---

## 0. Project Setup

- [ ] Initialize Spring Boot project
- [ ] Configure Gradle dependencies
- [ ] Set up package structure (domain-based)
- [ ] Configure application.yml (dev / prod)
- [ ] Configure database connection (MySQL)
- [ ] Configure Redis connection
- [ ] Add global exception handler
- [ ] Add base response format (success/error)
- [ ] Configure logging

---

## 1. Authentication System

### Core Auth
- [ ] Implement signup API
- [ ] Implement login API
- [ ] Implement OAuth login (Kakao, Naver, Google)
- [ ] Implement JWT authentication (access + refresh)
- [ ] Implement logout
- [ ] Implement token refresh

### Phone Verification
- [ ] Send verification code API
- [ ] Verify code API
- [ ] Store verification state
- [ ] Enforce phone verification in core actions

---

## 2. User System

- [ ] Create user entity
- [ ] Create user_auth entity
- [ ] Create user_trust entity
- [ ] Create user_level entity
- [ ] Implement get my profile API
- [ ] Implement update profile API
- [ ] Implement get user status API
- [ ] Implement user activity summary

---

## 3. Region System

- [ ] Create region_city / district / dong tables
- [ ] Seed region data
- [ ] Implement region lookup APIs
- [ ] Implement GPS-based region verification
- [ ] Implement primary region assignment
- [ ] Implement region change API
- [ ] Implement region change policy validation

---

## 4. Place System (Core Domain)

### Basic Structure
- [ ] Create place entity
- [ ] Create place_images entity
- [ ] Create place_stats entity

### Features
- [ ] Implement place creation API
- [ ] Validate place registration policy
- [ ] Implement place detail API
- [ ] Implement nearby places API
- [ ] Implement region-based place list
- [ ] Implement place search API
- [ ] Implement update place API
- [ ] Implement delete place API

---

## 5. Recommendation System

- [ ] Create recommendation entity
- [ ] Enforce unique (user, place)
- [ ] Implement recommend API
- [ ] Implement cancel recommendation API
- [ ] Validate daily recommendation limit
- [ ] Apply trust-based weight
- [ ] Update place_stats on recommendation
- [ ] Implement recommendation policy API

---

## 6. Visit System (Critical)

- [ ] Create visit entity
- [ ] Implement GPS validation logic
- [ ] Implement visit API
- [ ] Enforce visit cooldown (24h default)
- [ ] Store visit validity
- [ ] Update place_stats on visit
- [ ] Update user trust/level on visit
- [ ] Implement visit policy API

---

## 7. Comment System

- [ ] Create comment entity
- [ ] Enforce one comment per user per place
- [ ] Implement create comment API
- [ ] Implement update comment API
- [ ] Implement delete comment API
- [ ] Implement place comment list API
- [ ] Implement user comment list API

---

## 8. Ranking System

### Structure
- [ ] Create seasons table
- [ ] Create place_season_scores table
- [ ] Create ranking history table

### Logic
- [ ] Implement score calculation service
- [ ] Implement ranking aggregation logic
- [ ] Implement ranking read API (dong/district/city)
- [ ] Implement season handling

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

- [ ] Create system_policies table
- [ ] Implement policy loading service
- [ ] Implement policy cache (Redis)
- [ ] Replace hardcoded values with policy values

### Policies to Support
- [ ] daily recommendation limit
- [ ] visit radius
- [ ] visit cooldown
- [ ] ranking weights
- [ ] trust weights
- [ ] region change cooldown
- [ ] place registration limit

---

## 11. Report System

- [ ] Create report entity
- [ ] Implement create report API
- [ ] Implement user report list API
- [ ] Integrate report with admin workflow

---

## 12. Admin System (Critical)

### Admin Base
- [ ] Implement admin authentication/authorization
- [ ] Separate admin API routes

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

- [ ] Implement season creation
- [ ] Implement season status change
- [ ] Implement ranking recalculation trigger
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
- [ ] Implement admin action logs
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
