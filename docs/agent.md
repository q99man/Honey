# 🐝 Honeytong Project - Agent Instructions

---

## 1. Project Overview

Honeytong is a location-based local restaurant discovery platform.

This is NOT a simple CRUD application.

This system is:
- exploration-driven
- trust-based
- region-aware
- ranking-focused
- policy-controlled

Users act as bees exploring local restaurants (flowers).  
The system emphasizes real-world actions such as visiting places.

---

## 2. Core Philosophy

### 2.1 Exploration First
- Visiting is more important than registering
- Real-world behavior has higher value than passive actions

### 2.2 Trust Over Activity
- Trust determines influence
- Activity alone must not dominate ranking

### 2.3 Policy-Driven System
- All limits must come from configurable policies
- Avoid hardcoding values

### 2.4 Admin-Controlled System
- Admin must be able to control and override system behavior
- System must be operable without code changes

---

## 3. Architecture Principles

### 3.1 Monolithic Structure (MVP)
- Use Spring Boot monolithic architecture
- Separate domains clearly by package

### 3.2 Domain-Oriented Design
Organize code by domain, not by technical layers.

Example:
- user
- region
- place
- recommendation
- visit
- comment
- ranking
- mission
- report
- admin
- policy

---

## 4. Layered Structure

Each domain must follow:

- Controller → handles request/response only
- Service → contains business logic
- Repository → data access
- Entity → database model

### Rules
- Controllers must be thin
- Business logic must be inside services
- No direct DB logic in controllers

---

## 5. Coding Rules

### DO:
- Use DTOs for all requests and responses
- Validate all inputs in service layer
- Use clear method naming
- Write small, single-responsibility methods
- Handle edge cases explicitly

### DO NOT:
- Put business logic in controllers
- Hardcode business rules
- Mix multiple domains in one service
- Skip validation logic
- Return entity directly in API

---

## 6. Key Business Constraints

### Recommendation
- One recommendation per user per place
- Daily limit applies
- Weight depends on trust

### Visit
- GPS validation required
- Must be within allowed radius
- One visit per cooldown period

### Comment
- One comment per user per place

### Place Registration
- Limited by policy
- Restricted to local restaurants

---

## 7. Validation Strategy

All validation must be server-side.

Validation includes:
- authentication
- phone verification
- region verification
- duplication checks
- policy checks
- cooldown checks

Never trust frontend validation.

---

## 8. Policy System

### Principle
All important values must come from system policies.

### Examples
- daily recommendation limit
- visit radius
- visit cooldown
- ranking weights
- trust weights
- region change cooldown

### Implementation Rule
- Load policies from DB
- Cache policies when needed
- Do NOT hardcode values

---

## 9. Ranking Strategy

### Key Idea
Ranking is not calculated purely in real-time.

### Approach
- Store activity events (recommendation, visit, comment)
- Aggregate scores separately
- Use pre-calculated scores for ranking queries

### Priority
visit > recommendation > comment

---

## 10. Error Handling

### Rules
- Use consistent error response format
- Use clear error codes
- Do not expose internal details

### Example Error Codes
- UNAUTHORIZED
- PHONE_VERIFICATION_REQUIRED
- REGION_VERIFICATION_REQUIRED
- DAILY_LIMIT_EXCEEDED
- COOLDOWN_ACTIVE
- DUPLICATE_ACTION
- POLICY_VIOLATION

---

## 11. Security Rules

- Use JWT for authentication
- Validate user identity on every request
- Validate permissions for admin endpoints
- Prevent duplicate actions
- Prevent abuse through validation and policy

---

## 12. Admin System Rules

- Admin can override any data
- Admin can modify policies
- Admin actions must be logged
- Admin APIs must be separated from user APIs

---

## 13. Data Integrity Rules

- Prevent duplicate actions (recommend, visit, comment)
- Ensure idempotent operations when needed
- Validate all state transitions

---

## 14. Performance Strategy

- Avoid heavy real-time calculations
- Use aggregated tables for ranking
- Use caching for frequently accessed data
- Optimize queries for list endpoints

---

## 15. Logging Strategy

Log important actions:
- recommendation
- visit
- report
- admin actions

Logs should help debugging and monitoring.

---

## 16. Development Priority

Follow this order:

1. Authentication
2. User system
3. Region system
4. Place system
5. Recommendation
6. Visit
7. Comment
8. Ranking
9. Admin system
10. Optimization

---

## 17. Testing Strategy

- Validate core business rules first
- Test edge cases
- Test policy-driven logic
- Test duplication prevention

---

## 18. Code Quality Expectations

- Code must be readable and maintainable
- Avoid unnecessary complexity
- Prefer clarity over cleverness
- Keep functions small and focused

---

## 19. Important Mindset

This project is not about writing endpoints.

This project is about:
- enforcing rules
- controlling behavior
- managing trust
- enabling exploration

Always think in terms of:
- rules
- policies
- user behavior
- system integrity

---

## 20. Final Instruction

When implementing features:

1. Understand the rule from rules.md
2. Check constraints from prd.md
3. Apply logic in service layer
4. Use policy values from DB
5. Validate all edge cases

Never assume behavior.
Always enforce rules explicitly.