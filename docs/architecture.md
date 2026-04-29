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
6.2 Visit Flow
Client
 → POST /visits
 → validate GPS distance
 → validate cooldown
 → save visit
 → update place_stats
 → update user_level / trust
 → update audience stats
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
9. Policy System
9.1 Core Idea

All important values must be configurable.

9.2 Flow
Admin updates policy
 → DB updated
 → cache invalidated
 → new requests use updated value
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
