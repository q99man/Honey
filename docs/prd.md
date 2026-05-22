# 🐝 Honeytong PRD

## 1. Product Overview

Honeytong is a location-based local restaurant discovery platform focused on exploration, trust, and regional community.

Users act as bees exploring good local restaurants called flowers.  
The service is not a generic restaurant review app.  
It is an exploration-first community platform where users discover, visit, recommend, and grow through local activity.

The product is mobile-first.  
Web is secondary and mainly supports browsing, ranking, and community usage.

---

## 2. Core Concept

### 2.1 World Concept
- User = Bee
- Restaurant = Flower
- Recommendation = Honey
- Ranking = Honeycomb Guide / Star System
- Exploration = Collecting honey from flowers

### 2.2 Product Identity
This product combines the following ideas:
- local community
- map-based exploration
- trust-based participation
- light game mechanics

### 2.3 Core Philosophy
- Exploration is more important than registration
- Real visits are more important than simple reactions
- Local trust is more important than anonymous scale
- Policy-driven operation is more important than hardcoded rules

---

## 3. Product Goals

### 3.1 Main Goals
- Help users discover real local restaurants worth revisiting
- Build a trusted local exploration community
- Encourage active participation through ranking, growth, and missions
- Create a Korea-style local guide service with a distinctive identity

### 3.2 MVP Goals
- Allow users to browse, visit, recommend, and comment on local restaurants
- Establish trust with phone verification and region verification
- Provide regional rankings by city, district, and dong
- Provide admin controls to operate the platform without code changes

---

## 4. Target Users

### 4.1 Primary Users
- mobile users in Korea
- users who enjoy finding hidden local restaurants
- users interested in nearby real-world exploration
- users who prefer community-trusted recommendations over generic ratings

### 4.2 Secondary Users
- foreigners living in or visiting Korea
- Japanese-speaking users
- English-speaking users
- users who browse local rankings on web

---

## 5. Core User Experience

### 5.1 Main Experience
A user opens the app, verifies their region, explores nearby flowers on the map, visits a place, verifies the visit, recommends it, leaves a short comment, gains experience, and contributes to local rankings.

### 5.2 Emotional Experience
The service should feel like:
- exploration
- discovery
- recognition
- local belonging

### 5.3 Product Principle
This is not a long-form review platform.  
This is a short, activity-driven, trust-based local discovery platform.

---

## 6. Core Systems

## 6.1 Authentication System
Users can join by:
- local signup
- Kakao login
- Naver login
- Google login

Phone verification is required for core actions.

### Core Actions Requiring Phone Verification
- recommendation
- comment
- place registration
- ranking participation
- mission participation

Phone verification should be encouraged immediately after signup through popup or banner UX.

---

## 6.2 Region System

### Purpose
The region system defines user belonging and local identity.

### Structure
Regions are organized by:
- city
- district
- dong

Each user has one primary verified region.

### Principles
- users can explore anywhere
- users belong to one primary local region
- local ranking and registration are tied to region policy
- region change is flexible in MVP
- stricter region rules can be added later through admin settings

### Region Verification
- GPS-based verification
- region is mapped to administrative dong
- verified region becomes the user’s primary local region

---

## 6.3 Trust System

### Purpose
The trust system prevents abuse and increases data quality.

### Key Idea
Level and trust are different:
- Level = activity growth
- Trust = reliability and influence

### Trust Stages
Temporary names can be changed later, but current draft stages are:
- Seed Bee
- Verified Bee
- Local Bee
- Active Bee
- Trusted Bee
- Influencer Bee

### Trust Effects
Trust affects:
- recommendation weight
- ranking influence
- feature access
- registration permissions
- community credibility

### Trust Inputs
Trust can increase through:
- phone verification
- region verification
- valid visit activity
- healthy participation
- low report rate

Trust can decrease through:
- confirmed reports
- sanctions
- suspicious activity
- repeated abnormal patterns

---

## 6.4 Level and Growth System

### Purpose
The growth system rewards exploration and participation.

### Main Growth Sources
- daily attendance
- recommendation
- valid visit verification
- restaurant registration
- mission completion
- receiving recommendation on registered place

### Level Rewards
As level increases, users may receive:
- upgraded bee avatar stage
- titles
- more registration capacity
- stronger local recognition
- access to expanded participation features

### Important Rule
Growth should encourage exploration first, not only content posting.

---

## 6.5 Place System

### Concept
Places are flowers.  
A place is a local restaurant that users can discover, recommend, and help grow.

### Place Registration Fields
- name
- region
- coordinates
- address
- category
- price range
- recommended menu
- short recommendation sentence
- feature description
- image

### Place Restrictions
- franchise is restricted
- highly local places are preferred
- registration capacity is limited by policy
- registration scope can be tied to verified region

### Flower Growth
Flowers grow based on:
- recommendations
- valid visits
- comments
- recent activity
- diversity of users
- trust-weighted activity

---

## 6.6 Recommendation System

### Purpose
Recommendation signals that a place is worth revisiting and worth sharing.

### Definition
A good place is:
> a place worth revisiting when someone comes to this region

### Rules
- one recommendation per user per place
- daily recommendation limit applies
- recommendation weight depends on trust level
- recommendations are positive-only in MVP
- no dislike system in MVP

### Reasoning
Negative voting can easily create abuse, emotional conflict, and unfair attacks in a local service.

---

## 6.7 Visit Verification System

### Purpose
Visit is the most trusted action in the service.

### Verification Method
- GPS is required
- photo is optional

### Rules
- visit must be within allowed radius
- default radius is controlled by policy
- only one valid visit per user per place within cooldown period
- MVP default cooldown is 24 hours

### Importance
Visit has higher ranking importance than simple recommendation.

---

## 6.8 Comment System

### Purpose
Comments provide short community context without turning the product into a long review platform.

### Rules
- one comment per user per place
- short text only
- comment count can affect flower activity score
- comments are opinion-based, not score-based
- comment wars should be prevented

### Direction
Comments should remain lightweight, useful, and easy to moderate.

---

## 6.9 Ranking System

### Purpose
The ranking system gives recognition to popular local flowers and active bees.

### Ranking Units
- dong ranking
- district ranking
- city ranking

### Ranking Inputs
Ranking score is based on:
- recommendation score
- visit score
- comment score
- recency bonus
- diversity bonus
- trust bonus

### Priority
visit > recommendation > comment

### Season System
The ranking system is season-based.

MVP recommendation:
- monthly season by default

Admin should be able to change season settings later.

### Ranking History
Past rankings should be preserved so users can see historical star changes over time.

Example:
- previously 2 stars
- currently 1 star

---

## 6.10 Star System

### Purpose
The star system acts like a local guide identity.

### Rules
- dong #1 = 1 star
- district #1 = 2 stars
- city #1 = 3 stars

Stars are based on ranking result within the current season.

Historical star records should also be stored.

---

## 6.11 Audience Tag System

### Purpose
The product should support data-driven audience tags such as:
- popular among women in their 20s
- popular among men in their 30s and 40s
- popular with foreigners

### Data Basis
Audience tags should be generated from aggregated user activity:
- visits
- recommendations
- demographics such as age group, gender, nationality

### Important Principle
This system should use aggregate data only.  
It should not expose individual personal information.

---

## 6.12 Mission System

### Purpose
Missions improve retention and encourage repeated exploration.

### Possible Mission Types
- daily missions
- weekly missions
- seasonal missions
- local missions

### Example Actions
- visit flowers
- recommend places
- complete comments
- discover hidden local flowers

Mission detail can be simple in MVP and expanded later.

---

## 6.13 Report System

### Purpose
The report system protects trust and content quality.

### Report Targets
- user
- place
- comment

### Reasons
Examples:
- franchise violation
- spam
- abuse
- offensive content
- fake information

### Handling
Reports must be reviewed and managed through admin tools.

---

## 6.14 Admin System

### Importance
The admin system is one of the most important systems in this product.

This product must be policy-driven and operator-controlled.

### Admin Goals
- manage abuse
- manage content quality
- control ranking fairness
- adjust policies without code changes
- manage season and scoring settings

### Admin Areas
- dashboard
- user management
- place management
- recommendation and visit management
- report handling
- ranking control
- region policy management
- global policy settings
- admin action logs

### Core Principle
All important values should be adjustable by admin settings.

Examples:
- daily recommendation limit
- visit radius
- visit cooldown
- trust weights
- ranking weights
- season period
- region change cooldown
- registration scope

---

## 7. MVP Scope

## 7.1 Must Have
- signup/login
- phone verification
- region verification
- map-based place browsing
- place registration
- recommendation
- GPS-based visit verification
- comment
- regional ranking
- trust and growth basics
- admin dashboard basics
- report handling
- policy settings basics

## 7.2 Nice to Have After MVP
- advanced mission system
- notification system
- automatic abuse detection
- stronger audience analytics
- richer admin adjustments
- richer historical ranking views

---

## 8. Product Boundaries

### What This Product Is
- a local discovery platform
- an exploration-first restaurant community
- a trusted map-based ranking system
- a game-like local guide service

### What This Product Is Not
- a generic review website
- a pure delivery or reservation platform
- a long-form blogging platform
- a national franchise recommendation app

---

## 9. Success Criteria

### Product Success Signals
- users complete region verification
- users perform valid visits
- recommendations and visits continue to grow
- regional rankings feel alive
- local places receive repeated engagement
- admin can control the platform without developer intervention

### MVP Success Signals
- core flows work end-to-end
- trust and visit systems prevent obvious abuse
- ranking system produces believable local results
- admin tools are sufficient to operate the platform manually

---

## 10. Final Summary

Honeytong is a mobile-first, location-based, local restaurant exploration platform.

Its identity comes from five things:
- exploration-first experience
- trust-based participation
- local region structure
- ranking and star system
- admin-driven policy control

The product should start simple as an MVP, but it must be designed from the beginning so that admins can expand and tune the service later without breaking its core philosophy.