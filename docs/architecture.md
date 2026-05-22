# 🏗 Architecture

This document describes the system architecture of Honeytong.

The architecture is designed for:
- fast MVP development
- policy-driven control
- admin-managed operation
- scalable ranking and analytics system

---

## 1. System Overview

Honeytong is a mobile-first, location-based platform.

Core characteristics:
- real-world interaction (visit verification)
- trust-based influence system
- regional ranking system
- admin-controlled policies

---

## 2. High-Level Architecture

```text
[ Mobile App ]           [ Web App ]
      │                       │
      └──────────┬────────────┘
                 │
          [ API Gateway / Nginx ]
                 │
        [ Spring Boot Backend ]
                 │
   ┌─────────────┼─────────────┐
   │             │             │
[ MySQL ]   [ Redis ]   [ Object Storage ]
   │             │             │
   │             │             └─ Image Storage
   │             └─ Cache / Cooldown / Policies
   │
   └─ Core Data Storage

          [ Batch / Scheduler ]
                 │
    Ranking / Aggregation / Policy Evaluation
```
3. Frontend Architecture
3.1 Mobile App (Primary)

Responsibilities:

map-based exploration
visit verification
recommendation
comment
ranking view
user growth tracking

Design principle:

minimal logic
server-driven validation
3.2 Web App (Secondary)

Responsibilities:

browsing places
ranking display
community usage
partial user features
3.3 Admin Web

Responsibilities:

user management
place moderation
report handling
policy management
ranking control

Admin UI must be clearly separated from user UI.

4. Backend Architecture
4.1 Architecture Style
Spring Boot Monolithic Architecture
Domain-oriented design
Service-layer driven logic

Reason:

faster MVP development
easier coordination of rules and policies
simpler deployment
4.2 Domain Structure
com.honeytong

├── auth
├── user
├── region
├── place
├── recommendation
├── visit
├── comment
├── ranking
├── mission
├── report
├── admin
├── policy
├── batch
├── notification
└── common
4.3 Layered Structure

Each domain follows:

Controller → request/response handling
Service → business logic
Repository → data access
Entity → database mapping

Rules:

controllers must be thin
services contain all business logic
repositories handle only DB access
5. Core Data Storage
5.1 MySQL

Primary database for:

users
regions
places
recommendations
visits
comments
rankings
reports
policies
admin logs

Reason:

relational consistency
strong admin query support
5.2 Redis

Used for:

phone verification codes
cooldown management
daily limits
policy caching
hot ranking cache

Current baseline:
- Redis connection settings are environment-driven
- Redis-backed cache and cooldown behavior is disabled by default
- domains must explicitly adopt Redis through service boundaries before using it
- Redis health checks remain disabled by default until Redis is required for runtime readiness
- PolicyService can cache active policy values in Redis when `APP_REDIS_ENABLED=true`; DB policy rows remain authoritative.
- Recommendation daily limit checks can cache per-user daily active recommendation counts in Redis while recommendation rows remain authoritative.
- Visit cooldown checks can cache per-user/place cooldown-until timestamps in Redis while visit rows remain authoritative.
- Phone verification can cache latest unverified code state and attempt count in Redis while verification rows remain authoritative.
- Public place ranking reads can cache `place_season_scores` responses in Redis while season score rows remain authoritative.

Examples:

recommend:daily:user:{userId}
visit:cooldown:user:{userId}:place:{placeId}
policy:{key}
5.3 Object Storage

Used for:

place images
visit images
user profile images

Rules:

only store URLs in DB
never store binary data in DB
6. Core Execution Flows
6.1 Recommendation Flow
Client
 → POST /recommend
 → validate phone verification
 → validate duplication
 → validate daily limit
 → save recommendation
 → update place_stats
 → update user_level / trust
Current implementation:
- `recommend.daily_limit` is read from `system_policies`.
- Daily used count is loaded through `RecommendationDailyCounter`, which can cache the DB count in Redis when enabled.
- Successful recommend, cancel, and admin invalidation evict the user's current-day counter key so the next check reloads from durable recommendation rows.

6.2 Visit Flow
Client
 → POST /visits
 → validate GPS distance
 → validate cooldown
 → save visit
 → update place_stats
 → update user_level / trust
 → update audience stats
Current implementation:
- `visit.radius_meter` and `visit.cooldown_hour` are read from `system_policies`.
- Cooldown status is loaded through `VisitCooldownCache`, which can cache the DB-backed cooldown-until timestamp in Redis when enabled.
- Successful valid visits and admin visit invalidation evict the affected user/place cooldown key so the next check reloads from durable visit rows.

6.3 Place Registration Flow
Client
 → POST /places
 → validate user eligibility
 → validate region policy
 → validate franchise rules
 → save place
 → initialize stats
6.4 Ranking Read Flow
Client
 → GET /rankings
 → read from place_season_scores
 → return sorted results

Current implementation:
- ranking reads resolve season and region, then load public place ranking responses through `RankingCache`.
- Redis-backed ranking read caching is opt-in with `APP_REDIS_ENABLED=true` and `RANKING_CACHE_TTL`.
- Admin ranking recalculation and ranking exclusion state changes evict public place ranking cache entries.
- place ranking history reads validate normal public place visibility, then read finalized snapshots from `place_ranking_history`.
- Flyway migration `V2__add_ranking_query_indexes.sql` adds indexes for public ranking, history finalization, and place history read paths.

Important:

never calculate ranking on the fly
always use aggregated data
7. Ranking Architecture
7.1 Strategy
event-based recording
aggregation-based ranking
read from precomputed tables
7.2 Score Composition
recommendation score
visit score
comment score
recency bonus
diversity bonus
trust bonus
manual admin adjustment

Manual admin adjustment is stored separately from automatic activity aggregates and is applied during ranking recalculation, not during public ranking reads.
Admin ranking exclusion is stored on each place and removes that place from ranking recalculation without changing normal place exposure.
7.3 Priority

visit > recommendation > comment

8. Batch and Scheduler
8.1 Purpose

Batch jobs handle:

ranking calculation
season finalization
audience tag generation
trust recalculation
statistics aggregation
8.2 Execution Timing

Examples:

hourly → activity aggregation
daily → score adjustments
season end → ranking finalization
8.3 MVP Approach
use Spring Scheduler
no separate worker service initially

Current implementation:
- ranking recalculation logic lives in `RankingRecalculationService` so admin manual recalculation and scheduled recalculation share the same aggregation path
- ranking history finalization rewrites `place_ranking_history` from `place_season_scores` for one selected season through an explicit admin operation
- public place ranking history reads use finalized `place_ranking_history` rows and do not read current `place_season_scores`
- `RANKING_SCHEDULER_ENABLED=false` keeps the scheduled job disabled by default for local development and production safety
- `RANKING_SCHEDULER_CRON`, `RANKING_SCHEDULER_ZONE`, and optional `RANKING_SCHEDULER_SEASON_CODE` control the schedule when an environment explicitly enables it
- admin-triggered recalculation remains the primary audited operation path; scheduled recalculation logs application runtime messages but does not create `admin_action_logs`
9. Policy System
9.1 Core Idea

All important values must be configurable.

9.2 Flow
Admin updates policy
 → DB updated
 → cache invalidated
 → new requests use updated value
Current implementation:
- `PolicyService` reads active `system_policies` rows and stores policy value plus value type through a `PolicyCache` boundary.
- Redis-backed policy caching is opt-in with `APP_REDIS_ENABLED=true` and `POLICY_CACHE_TTL`.
- When Redis usage is disabled, a no-op cache keeps all policy reads on the DB-backed path.
- Admin policy updates evict only the changed policy key after the DB value is updated and logged.

9.3 Examples
daily recommendation limit
visit radius
visit cooldown
ranking weights
trust weights
10. Admin Architecture
10.1 Core Principle

Admin must control:

content
users
policies
ranking
10.2 Key Features
dashboard metrics
user moderation
place moderation
report handling
policy editing
ranking recalculation
10.3 Roles
ADMIN → operations
SUPER_ADMIN → policy control
11. Security Architecture
11.1 Authentication
JWT-based authentication
access token + refresh token
Phone verification current implementation:
- verification code hashes, expiry, and verified state are stored in `phone_verification_codes`
- `PhoneVerificationCache` can cache the latest unverified code state in Redis when enabled
- successful verification evicts the cached phone verification key after the DB row and user state are updated
- phone code delivery is selected by `PHONE_VERIFICATION_SENDER_PROVIDER`
- the default development sender logs issuance without the raw code at INFO level
- the SOLAPI sender can send production SMS messages with environment-provided API key, API secret, registered sender number, and message template
- the Naver Cloud SENS sender can send production SMS messages with environment-provided service id, access key, secret key, sender number, and message template
- the production profile defaults `PHONE_VERIFICATION_SENDER_PROVIDER` to `solapi`; local development defaults to `dev`
11.2 Authorization
role-based access control
admin APIs protected
11.3 Validation

All critical validations must be server-side:

phone verification
region verification
duplication prevention
cooldown enforcement
12. Data Integrity Strategy
prevent duplicate actions
enforce unique constraints
validate state transitions
ensure idempotency when needed
13. Performance Strategy
avoid real-time heavy calculations
use aggregated tables
cache frequently accessed data
optimize list queries
14. Logging Strategy
14.1 Application Logs
errors
validation failures
security events
runtime diagnostics

Current baseline:
application log levels, file path, and rotation settings are environment-driven
SQL bind logging is disabled by default
raw access tokens, refresh tokens, and verification codes should not be logged at normal levels
14.2 Business Logs
recommendations
visits
reports
admin actions
15. Deployment Architecture
MVP Deployment
frontend (mobile/web)
backend (Spring Boot)
MySQL database
Redis cache
object storage
health checks via Spring Boot Actuator `/actuator/health`

Production profile baseline:
- run the backend with `SPRING_PROFILES_ACTIVE=prod`
- provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` from the environment
- production defaults `spring.jpa.hibernate.ddl-auto` to `validate` so Hibernate does not mutate schema automatically
- region seed import, policy seed import, and local admin bootstrap stay disabled by default

Database migration baseline:
- Flyway manages production schema changes before Hibernate validation
- migration files live under `backend/src/main/resources/db/migration`
- migration filenames use `V{number}__{description}.sql`, starting with `V1__baseline_schema.sql`
- local development keeps Flyway disabled by default unless `FLYWAY_ENABLED=true` is set intentionally
- existing non-empty databases without Flyway history should be baselined deliberately after backup and review, not silently migrated

Redis configuration baseline:
- provide `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, and `REDIS_TIMEOUT` from the environment when Redis is used
- keep `CACHE_TYPE=none` and `APP_REDIS_ENABLED=false` unless a runtime environment intentionally enables an adopted Redis boundary
- Redis repository auto-configuration is disabled because Honeytong does not use Redis repositories in the MVP baseline
- Redis health checks are disabled by default and should be enabled only when Redis becomes a required runtime dependency
- `POLICY_CACHE_TTL` controls the Redis TTL for cached policy values and defaults to 10 minutes
- visit cooldown Redis keys use the remaining cooldown duration as their TTL and are evicted on visit state changes
- phone verification Redis keys use the remaining verification-code lifetime as their TTL and are evicted after successful verification
- `RANKING_CACHE_TTL` controls the Redis TTL for public place ranking read responses and defaults to 5 minutes

Ranking scheduler configuration baseline:
- keep `RANKING_SCHEDULER_ENABLED=false` unless an environment intentionally wants automatic recalculation
- `RANKING_SCHEDULER_CRON` defaults to `0 0 4 * * *`
- `RANKING_SCHEDULER_ZONE` defaults to `Asia/Seoul`
- `RANKING_SCHEDULER_SEASON_CODE` can target a specific season; blank means the current active season

Scaling Strategy

Stage 1:

monolith + scheduler

Stage 2:

separate batch processing
improve caching

Stage 3:

advanced analytics
abuse detection systems
16. Development Philosophy

This system is not CRUD-driven.

It is:

rule-driven
policy-driven
behavior-driven

Developers must always consider:

business rules
trust system
ranking impact
admin control
17. Final Notes

The most important architectural decision is:

keep the system simple for MVP, but structure it so that policies, ranking, and admin control can evolve without breaking the system.

This ensures:

fast development
stable operation
flexible growth
