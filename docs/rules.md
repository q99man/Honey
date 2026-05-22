# 🧠 Honeytong Business Rules

---

## 1. General Principles

- All business rules must be enforced on the server side
- No critical rule should rely only on frontend validation
- All limits must be configurable via system policies
- Trust and activity both influence the system

---

## 2. Authentication Rules

- Email or SNS login is allowed
- Phone verification is required for core actions

### Core Actions
- place registration
- recommendation
- comment
- visit verification
- ranking participation
- mission participation

---

## 3. Region Rules

- Each user has one primary region (dong)
- Region must be verified via GPS
- Users can explore all regions
- Place registration may be restricted by region policy

### Region Change
- Region change is allowed
- Cooldown is controlled by policy
- MVP can allow flexible region changes

---

## 4. Place Registration Rules

- A user can register limited number of places
- Registration limit depends on policy and level
- Registration scope can be:
  - dong
  - district
  - city

### Restrictions
- Franchise places are restricted
- Non-local places should be rejected or flagged
- Admin can override any place

---

## 5. Recommendation Rules

- One recommendation per user per place
- Recommendation is positive-only (no dislike in MVP)
- Daily recommendation limit applies
- Recommendation weight depends on user trust

### Invalid Cases
- duplicate recommendation → reject
- user not phone verified → reject

---

## 6. Visit Rules

- Visit must be verified via GPS
- Distance must be within allowed radius
- Radius is defined by policy

### Cooldown
- One valid visit per user per place within cooldown period
- Default cooldown: 24 hours

### Invalid Cases
- out of radius → reject
- cooldown not expired → reject

---

## 7. Comment Rules

- One comment per user per place
- Comment must be short text
- Comment length limit applies

### Behavior
- Comment contributes to activity score
- Comment does not directly represent rating score

### Invalid Cases
- duplicate comment → reject
- offensive content → filter or block

---

## 8. Trust Rules

- Trust is separate from level
- Trust determines influence weight

### Trust Increase Factors
- phone verification
- region verification
- valid visit activity
- healthy participation

### Trust Decrease Factors
- confirmed reports
- admin sanctions
- abnormal behavior patterns

---

## 9. Level Rules

- Level is based on activity
- Level increases through:
  - visit
  - recommendation
  - comment
  - registration
  - mission

### Effects
- higher level may unlock:
  - more registration capacity
  - titles
  - visual upgrades

---

## 10. Ranking Rules

### Score Components
Score is calculated using:
- recommendation score
- visit score
- comment score
- recency bonus
- diversity bonus
- trust bonus

### Priority
visit > recommendation > comment

---

## 11. Season Rules

- Ranking is calculated per season
- Default season unit: monthly
- Season duration must be configurable

### History
- Ranking history must be preserved
- Past star levels must be stored

---

## 12. Star Rules

- Top place in dong → 1 star
- Top place in district → 2 stars
- Top place in city → 3 stars

- Star is determined by ranking result

---

## 13. Audience Tag Rules

- Tags are generated from aggregated data
- No personal data should be exposed

### Data Sources
- visits
- recommendations
- user demographics (aggregated)

---

## 14. Report Rules

- Users can report:
  - place
  - user
  - comment

### Report Handling
- Reports must be reviewed by admin
- Admin can:
  - hide content
  - delete content
  - sanction users

---

## 15. Admin Rules

- Admin can override any data
- Admin can adjust scores
- Admin can manage policies
- Admin actions must be logged

---

## 16. Policy Rules

- All configurable values must be stored in system policies

### Examples
- daily recommendation limit
- visit radius
- visit cooldown
- ranking weights
- trust weights
- region change cooldown
- registration limits

---

## 17. Abuse Prevention Rules

- Multiple accounts must be restricted via phone verification
- Suspicious behavior should be monitored
- Repeated abuse can lead to:
  - trust reduction
  - temporary restriction
  - permanent ban

---

## 18. Data Integrity Rules

- All critical actions must be idempotent
- Duplicate actions must be prevented
- Invalid state transitions must be blocked

---

## 19. Logging Rules

- Important actions must be logged:
  - recommendation
  - visit
  - report
  - admin actions

---

## 20. MVP Simplification Rules

- No dislike system
- No complex moderation automation
- No advanced fraud detection
- Admin handles edge cases manually

## 21. UI Language Rules

- All user-facing UI must be Korean by default
- Korean text must be UTF-8 safe
- English is allowed only for internal code and documentation
- If multilingual support is added, Korean remains the default source language
- Broken Korean text must be fixed before completing UI-related work