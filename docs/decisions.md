# 🧠 Architectural Decisions

This document records key decisions made during the design of Honeytong.

Its purpose is to:
- explain WHY decisions were made
- prevent inconsistent changes later
- provide context for future development
- guide refactoring and scaling

---

## 1. Monolithic Architecture (MVP)

### Decision
Use Spring Boot monolithic architecture.

### Reason
- faster MVP development
- simpler deployment
- easier coordination of policies and business rules

### Trade-off
- less scalable than microservices

### Future Plan
- separate batch processing if needed
- split domains only when complexity increases

---

## 2. Domain-Oriented Structure

### Decision
Organize code by domain, not by technical layer.

### Reason
- improves maintainability
- aligns with business logic
- easier to scale features independently

---

## 3. Policy-Driven System

### Decision
All critical values must be stored in DB policies.

### Examples
- recommendation limits
- visit radius
- cooldown durations
- ranking weights

### Reason
- admin must control system behavior without code changes
- enables fast iteration and tuning

### Trade-off
- increased complexity in implementation

---

## 4. Aggregation-Based Ranking

### Decision
Ranking is NOT calculated in real-time.

### Instead
- store activity events
- aggregate scores periodically
- read from precomputed tables

### Reason
- performance optimization
- predictable ranking results
- easier admin control

---

## 5. Trust-Based Influence System

### Decision
User actions are weighted by trust.

### Reason
- prevent abuse
- reward active and reliable users
- maintain ranking quality

### Trade-off
- requires additional tracking and calculation

---

## 6. Visit Verification with GPS

### Decision
Visits require GPS-based validation.

### Reason
- ensure real-world interaction
- increase credibility of recommendations

### Trade-off
- GPS spoofing risk
- device dependency

### Mitigation
- cooldown limits
- anomaly detection (future)

---

## 7. One Action Per User Rule

### Decision
- one recommendation per user per place
- one comment per user per place

### Reason
- prevent spam
- simplify logic
- maintain fairness

---

## 8. Cooldown-Based Controls

### Decision
Use cooldown instead of unlimited actions.

### Examples
- visit cooldown (24h)
- region change cooldown
- daily recommendation limit

### Reason
- control abuse
- encourage natural usage patterns

---

## 9. Region-Based System (Hyperlocal)

### Decision
System is built around:
- city
- district
- dong

### Reason
- hyperlocal discovery is core value
- aligns with Korean user behavior (당근 스타일)

---

## 10. Place Registration Limitation

### Decision
Users have limited place registration capacity.

### Reason
- prevent spam content
- increase quality of entries
- encourage thoughtful contribution

---

## 11. Franchise Restriction Policy

### Decision
Franchise places are restricted.

### Reason
- maintain "local hidden gem" concept
- differentiate from generic review platforms

### Trade-off
- requires moderation
- potential ambiguity

---

## 12. Admin-Centric Control System

### Decision
Admin has strong control over:
- users
- places
- reports
- policies
- rankings

### Reason
- early-stage system needs manual intervention
- abuse must be controllable

---

## 13. Mobile-First Design

### Decision
System is designed primarily for mobile.

### Reason
- location-based behavior
- real-time interaction
- map-based UX

---

## 14. Web as Secondary Platform

### Decision
Web supports browsing and admin usage.

### Reason
- mobile is primary usage
- admin tools require larger interface

---

## 15. Redis for Runtime Control

### Decision
Use Redis for:
- cooldown
- daily limits
- temporary states
- caching

### Reason
- fast access
- reduces DB load

---

## 16. Separation of User and Admin APIs

### Decision
Admin APIs must be separated from user APIs.

### Reason
- security
- clarity
- easier permission control

---

## 16.1 Access Token and Refresh Token Structure

### Decision
Use short-lived JWT access tokens and server-managed refresh tokens.

### Rules
- store only hashed refresh tokens
- rotate refresh tokens on every successful refresh
- revoke refresh tokens on logout
- keep token durations in configuration, not code

### Reason
- access tokens keep normal API requests stateless
- refresh tokens can be revoked when sessions must end
- rotation reduces damage from leaked refresh tokens

---

## 16.2 Phone Verification Delivery

### Decision
Use a sender interface for phone verification code delivery.

### Current Implementation
- development sender logs verification codes
- verification codes are stored as hashes
- code length, expiry, and max attempts are configurable
- core actions can use the server-side `@RequirePhoneVerified` guard

### Future Plan
- replace development sender with a production SMS provider adapter
- required external values will include provider access key, secret key, sender number, and message template settings

### Reason
- backend validation can be built before choosing the SMS provider
- production credentials do not need to be committed to the repository
- sender implementation can change without changing controller or service logic

---

## 16.3 Region GPS Resolver Boundary

### Decision
Separate GPS-to-administrative-dong resolution behind a `RegionCoordinateResolver` interface.

### Current Implementation
- region lookup APIs read from `region_city`, `region_district`, and `region_dong`
- GPS region verification flow saves the resolved dong as the user's primary region
- Kakao Local API `coord2regioncode` is used when `app.maps.provider=kakao`
- Kakao administrative region documents (`region_type = H`) are preferred
- Kakao region codes are matched against `region_dong.code`

### Reason
- administrative dong mapping requires reliable region boundary data or provider integration
- hardcoding coordinate-to-region rules would be inaccurate and difficult to maintain
- region verification must remain server-side and replaceable

---

## 16.4 Map Provider for MVP

### Decision
Use Kakao Map and Kakao Local API as the MVP map provider.

### Current Preparation
- API keys are loaded from local environment variables
- `.env` is ignored by Git and `.env.example` documents required variable names
- backend reads `KAKAO_REST_API_KEY`, `KAKAO_JAVASCRIPT_KEY`, and `KAKAO_LOCAL_BASE_URL`
- frontend can read `VITE_KAKAO_JAVASCRIPT_KEY`

### Reason
- Kakao Local API directly supports coordinate-to-region-code conversion
- Kakao Local API also supports keyword/category place search with coordinates and radius
- this fits region verification and nearby place discovery before the place domain is implemented

---

## 16.5 Region Seed Import

### Decision
Use an opt-in CSV import path for region seed data.

### Current Implementation
- import is disabled by default
- `REGION_SEED_ENABLED=true` enables import on backend startup
- `REGION_SEED_LOCATION` points to a UTF-8 CSV file
- CSV rows use city, district, and administrative dong codes
- `region_dong.code` must match Kakao administrative dong codes returned by `coord2regioncode`
- the full seed CSV is generated from `KIKcd_H.20260325.xlsx` using `scripts/convert_korean_region_seed.py`

### Reason
- region data is operational reference data and should not change accidentally on every development run
- the Kakao resolver needs stable administrative dong codes in DB
- the import path lets the project add a complete national seed file later without changing service logic

---

## 16.6 Region Change Policy

### Decision
Region change cooldown must be read from `system_policies`.

### Current Implementation
- `PATCH /api/regions/me` changes the user's primary region by `dongId`
- `GET /api/regions/me/change-policy` returns cooldown availability
- the required policy key is `region.change_cooldown_day`
- if the policy is missing or invalid, the server rejects region change

### Reason
- region change is a business policy and must remain admin-adjustable
- place registration, recommendation, visit, and ranking depend on stable primary region behavior

---

## 17. Server-Side Validation Only

### Decision
All critical validation must be done on server.

### Reason
- frontend cannot be trusted
- prevents abuse

---

## 18. No Hardcoded Business Rules

### Decision
Business rules must not be hardcoded.

### Reason
- flexibility
- easier tuning
- admin control

---

## 19. Event-Driven Data Collection

### Decision
Record user actions as events.

### Examples
- recommendation
- visit
- comment

### Reason
- enables analytics
- supports ranking aggregation

---

## 20. Simple MVP, Expandable Structure

### Decision
Keep MVP simple but structure scalable.

### Reason
- reduce initial complexity
- allow future expansion without redesign

---

## 21. Ranking Priority Design

### Decision
visit > recommendation > comment

### Reason
- real-world action is most valuable
- recommendation is secondary signal
- comments are supportive signals

---

## 22. Audience Tag System (Future-Ready)

### Decision
Design system to support demographic-based tags.

### Examples
- "Popular among men in their 30s"
- "Popular among women in their 20s"

### Reason
- enhances discovery
- adds insight beyond simple ranking

---

## 23. Season-Based Ranking System

### Decision
Ranking is calculated per season.

### Reason
- freshness
- competition
- historical tracking

---

## 24. Logging for Critical Actions

### Decision
Log:
- user actions
- admin actions

### Reason
- debugging
- monitoring
- abuse detection

---

## 25. Growth System (Level & EXP)

### Decision
Users gain EXP and level up.

### Reason
- gamification
- increase engagement

---

## 26. Soft Control Before Hard Restriction

### Decision
Start with flexible policies, tighten later.

### Reason
- early-stage needs data
- avoid blocking growth

---

## 27. Final Principle

The system is designed to:

- encourage exploration
- reward trust
- prevent abuse
- allow admin control
- remain flexible

---

## Final Note

Every new feature must respect these decisions.

If a change conflicts with a decision:
- document it here
- explain the reason
- update related systems accordingly
