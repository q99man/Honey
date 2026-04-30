# Database Schema

This document defines the database schema for Honeytong.

The schema is designed for:
- MVP-first implementation
- policy-driven operation
- admin-controlled configuration
- future ranking and analytics expansion

The database is based on MySQL.
The local development schema name is `honey`.

---

1. Design Principles

1.1 Core Domains

The schema is organized around these core domains:
- user
- authentication
- region
- place
- recommendation
- visit
- comment
- ranking
- trust and level
- report and admin
- system policy

1.2 MVP Strategy

The schema should support MVP first, while keeping extension points for:
- audience tags
- missions
- notification
- seasonal ranking history
- advanced abuse detection

1.3 Important Rule

Critical business rules must be enforced both by:
- database constraints where possible
- server-side business logic

Examples:
- one recommendation per user per place
- one comment per user per place
- unique phone number per active account

---

2. Region Tables

Region is a core system because the product is local-first.

2.1 region_city

Stores city-level administrative region.

```sql
CREATE TABLE region_city (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name_ko VARCHAR(100) NOT NULL,
  name_en VARCHAR(100),
  name_ja VARCHAR(100),
  code VARCHAR(20) UNIQUE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
```

2.2 region_district

Stores district-level administrative region.

CREATE TABLE region_district (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  city_id BIGINT NOT NULL,
  name_ko VARCHAR(100) NOT NULL,
  name_en VARCHAR(100),
  name_ja VARCHAR(100),
  code VARCHAR(20) UNIQUE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_region_district_city
    FOREIGN KEY (city_id) REFERENCES region_city(id)
);

2.3 region_dong

Stores dong-level administrative region.

CREATE TABLE region_dong (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  city_id BIGINT NOT NULL,
  district_id BIGINT NOT NULL,
  name_ko VARCHAR(100) NOT NULL,
  name_en VARCHAR(100),
  name_ja VARCHAR(100),
  code VARCHAR(20) UNIQUE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_region_dong_city
    FOREIGN KEY (city_id) REFERENCES region_city(id),
  CONSTRAINT fk_region_dong_district
    FOREIGN KEY (district_id) REFERENCES region_district(id)
);

Notes
code should store the Kakao administrative dong code when Kakao Local API is used for GPS verification
region seed data can be imported from a UTF-8 CSV file using the backend region seed importer
city and district codes are used for hierarchy import and lookup consistency

2.4 Region Seed Import

The backend supports an opt-in UTF-8 CSV import for region reference data.

CSV columns:

city_code,city_name_ko,district_code,district_name_ko,dong_code,dong_name_ko

Rules:
- import is disabled by default
- set REGION_SEED_ENABLED=true to run import on backend startup
- set REGION_SEED_LOCATION to a classpath or file resource
- dong_code must match the Kakao administrative dong code used by coord2regioncode
- `backend/src/main/resources/region/region-seed.csv` is generated from the Korean administrative dong source workbook

3. User Tables

3.1 users

Stores core user profile information.

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nickname VARCHAR(50) NOT NULL,
  email VARCHAR(150),
  phone VARCHAR(30),
  phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  birth_year INT,
  gender VARCHAR(20),
  nationality_code VARCHAR(10),
  profile_image_url VARCHAR(255),
  language_preference VARCHAR(10) DEFAULT 'ko',
  marketing_agreed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_users_phone (phone)
);
Notes
birth_year, gender, and nationality_code are used for aggregate audience analysis only
user-facing privacy must be handled at application level
role can be USER / ADMIN / SUPER_ADMIN

3.2 user_auth

Stores authentication provider information.

CREATE TABLE user_auth (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider VARCHAR(20) NOT NULL,
  provider_user_id VARCHAR(200),
  email VARCHAR(150),
  password_hash VARCHAR(255),
  last_login_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_user_auth_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_user_auth_provider (provider, provider_user_id)
);
Notes
provider values can be LOCAL, KAKAO, NAVER, GOOGLE
password_hash is used only for LOCAL login

3.2.1 refresh_tokens

Stores refresh tokens for access token renewal and logout.

CREATE TABLE refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(100) NOT NULL,
  expires_at DATETIME NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  revoked_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_refresh_tokens_hash (token_hash)
);
Notes
raw refresh tokens must never be stored
refresh tokens are rotated on refresh and revoked on logout

3.2.2 phone_verification_codes

Stores phone verification codes for authenticated users.

CREATE TABLE phone_verification_codes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  phone VARCHAR(30) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  verified_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_phone_verification_codes_user
    FOREIGN KEY (user_id) REFERENCES users(id)
);
Notes
raw verification codes must never be stored
code length, expiry, and max attempts are configurable
production SMS sending requires an external SMS provider

3.3 user_region

Stores verified region information for the user.

CREATE TABLE user_region (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  city_id BIGINT NOT NULL,
  district_id BIGINT NOT NULL,
  dong_id BIGINT NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT TRUE,
  verified_at DATETIME,
  changed_at DATETIME,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_user_region_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_region_city
    FOREIGN KEY (city_id) REFERENCES region_city(id),
  CONSTRAINT fk_user_region_district
    FOREIGN KEY (district_id) REFERENCES region_district(id),
  CONSTRAINT fk_user_region_dong
    FOREIGN KEY (dong_id) REFERENCES region_dong(id)
);
Notes
MVP assumes one primary region per user
this table can support region history later

3.4 user_trust

Stores trust-related user data.

CREATE TABLE user_trust (
  user_id BIGINT PRIMARY KEY,
  trust_score INT NOT NULL DEFAULT 0,
  trust_grade VARCHAR(30) NOT NULL DEFAULT 'SEED_BEE',
  recommend_weight DECIMAL(4,2) NOT NULL DEFAULT 1.00,
  sanction_count INT NOT NULL DEFAULT 0,
  report_received_count INT NOT NULL DEFAULT 0,
  report_confirmed_count INT NOT NULL DEFAULT 0,
  abnormal_activity_score INT NOT NULL DEFAULT 0,
  phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
  region_verified BOOLEAN NOT NULL DEFAULT FALSE,
  last_evaluated_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_user_trust_user
    FOREIGN KEY (user_id) REFERENCES users(id)
);
Notes
trust and level are intentionally separated
trust affects influence, not just activity count

3.5 user_level

Stores user level and experience.

CREATE TABLE user_level (
  user_id BIGINT PRIMARY KEY,
  level INT NOT NULL DEFAULT 1,
  exp INT NOT NULL DEFAULT 0,
  total_exp INT NOT NULL DEFAULT 0,
  title VARCHAR(50),
  avatar_stage VARCHAR(50),
  rank_score DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_user_level_user
    FOREIGN KEY (user_id) REFERENCES users(id)
);

3.6 user_level_history

Stores level-up history.

CREATE TABLE user_level_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  previous_level INT NOT NULL,
  new_level INT NOT NULL,
  changed_reason VARCHAR(100),
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_user_level_history_user
    FOREIGN KEY (user_id) REFERENCES users(id)
);

4. Place Tables

4.1 places

Stores restaurant (flower) data.

CREATE TABLE places (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  created_by BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  category_code VARCHAR(50) NOT NULL,
  region_city_id BIGINT NOT NULL,
  region_district_id BIGINT NOT NULL,
  region_dong_id BIGINT NOT NULL,
  address_road VARCHAR(255),
  address_jibun VARCHAR(255),
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  price_range_code VARCHAR(30),
  recommended_menu VARCHAR(255),
  short_recommendation VARCHAR(255) NOT NULL,
  feature_text VARCHAR(500),
  is_franchise BOOLEAN NOT NULL DEFAULT FALSE,
  franchise_review_status VARCHAR(20) DEFAULT 'PENDING',
  approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  exposure_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  current_star_level INT NOT NULL DEFAULT 0,
  current_flower_grade VARCHAR(30) DEFAULT 'SEED',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted_at DATETIME NULL,
  CONSTRAINT fk_places_user
    FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT fk_places_city
    FOREIGN KEY (region_city_id) REFERENCES region_city(id),
  CONSTRAINT fk_places_district
    FOREIGN KEY (region_district_id) REFERENCES region_district(id),
  CONSTRAINT fk_places_dong
    FOREIGN KEY (region_dong_id) REFERENCES region_dong(id)
);
Notes
approval_status supports future moderation policies
current_star_level and current_flower_grade support current state display
ranking history is stored separately
current_star_level is updated by admin-triggered ranking recalculation based on the best current season result

4.2 place_images

Stores images for a place.

CREATE TABLE place_images (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_id BIGINT NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_representative BOOLEAN NOT NULL DEFAULT FALSE,
  uploaded_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_place_images_place
    FOREIGN KEY (place_id) REFERENCES places(id),
  CONSTRAINT fk_place_images_user
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

4.3 place_stats

Stores aggregate place statistics for efficient reads.

CREATE TABLE place_stats (
  place_id BIGINT PRIMARY KEY,
  recommend_count INT NOT NULL DEFAULT 0,
  visit_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  unique_user_count INT NOT NULL DEFAULT 0,
  score_total DECIMAL(12,2) NOT NULL DEFAULT 0,
  recent_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  diversity_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  trust_weighted_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  last_activity_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_place_stats_place
    FOREIGN KEY (place_id) REFERENCES places(id)
);
Notes
do not calculate all ranking values on every read
this table is a read optimization and aggregation target

4.4 place_audience_stats

Stores aggregated demographic statistics for audience tags.

CREATE TABLE place_audience_stats (
  place_id BIGINT PRIMARY KEY,
  age_10_count INT NOT NULL DEFAULT 0,
  age_20_count INT NOT NULL DEFAULT 0,
  age_30_count INT NOT NULL DEFAULT 0,
  age_40_count INT NOT NULL DEFAULT 0,
  age_50_plus_count INT NOT NULL DEFAULT 0,
  male_count INT NOT NULL DEFAULT 0,
  female_count INT NOT NULL DEFAULT 0,
  other_gender_count INT NOT NULL DEFAULT 0,
  foreigner_count INT NOT NULL DEFAULT 0,
  korean_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_place_audience_stats_place
    FOREIGN KEY (place_id) REFERENCES places(id)
);
Notes
this table must only store aggregate counts
audience tags such as “popular among men in their 30s and 40s” are derived from here

4.5 place_tags

Stores visible tags for places.

CREATE TABLE place_tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_id BIGINT NOT NULL,
  tag_type VARCHAR(30) NOT NULL,
  tag_code VARCHAR(50) NOT NULL,
  tag_label_ko VARCHAR(100) NOT NULL,
  tag_label_en VARCHAR(100),
  tag_label_ja VARCHAR(100),
  source_type VARCHAR(30) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_place_tags_place
    FOREIGN KEY (place_id) REFERENCES places(id)
);
Notes
tag_type examples: SYSTEM, ADMIN
source_type examples: AUTO, MANUAL

5. Activity Tables

5.1 recommendations

Stores recommendation records.

CREATE TABLE recommendations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  recommend_weight DECIMAL(4,2) NOT NULL DEFAULT 1.00,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_recommendations_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_recommendations_place
    FOREIGN KEY (place_id) REFERENCES places(id),
  UNIQUE KEY uk_recommendations_user_place (user_id, place_id)
);
Notes
one user can recommend one place only once
soft status control is useful for cancellation and admin invalidation
the backend treats ACTIVE as the current recommendation state and CANCELED as user cancellation
daily recommendation limits are enforced with `system_policies.recommend.daily_limit`

5.2 visits

Stores valid or invalid visit attempts.

CREATE TABLE visits (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  distance_meter INT,
  image_url VARCHAR(255),
  is_valid BOOLEAN NOT NULL DEFAULT TRUE,
  valid_reason VARCHAR(100),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_visits_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_visits_place
    FOREIGN KEY (place_id) REFERENCES places(id)
);
Notes
visit cooldown should be enforced in server logic
storing is_valid allows later investigation or admin invalidation
the visit API currently stores valid visits and rejects out-of-radius or cooldown-active attempts
visit radius is enforced with `system_policies.visit.radius_meter`
visit cooldown is enforced with `system_policies.visit.cooldown_hour`
my visit history reads valid rows from this table
place visit summary reads aggregate count from `place_stats` and latest valid visit time from this table

5.3 comments

Stores one comment per user per place.

CREATE TABLE comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  content VARCHAR(300) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  report_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted_at DATETIME NULL,
  CONSTRAINT fk_comments_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_comments_place
    FOREIGN KEY (place_id) REFERENCES places(id),
  UNIQUE KEY uk_comments_user_place (user_id, place_id)
);
Notes
comment is lightweight and short-form by design
one user can only keep one active comment per place
the backend restores a deleted comment row when the same user writes again for the same place
visible comments update `place_stats.comment_count`
comment score weight is read from `system_policies` key `ranking.comment_weight`

6. Ranking and Season Tables

6.1 seasons

Stores ranking season configuration.

CREATE TABLE seasons (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  season_code VARCHAR(30) NOT NULL,
  season_name VARCHAR(100) NOT NULL,
  season_type VARCHAR(20) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_seasons_code (season_code)
);
Notes
default season type can be MONTHLY
must support future quarter or yearly seasons

6.2 place_season_scores

Stores per-season ranking score by region.

CREATE TABLE place_season_scores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  season_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  region_type VARCHAR(20) NOT NULL,
  region_ref_id BIGINT NOT NULL,
  recommend_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  visit_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  comment_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  recent_bonus_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  diversity_bonus_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  trust_bonus_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  total_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  rank_no INT,
  star_level INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_place_season_scores_season
    FOREIGN KEY (season_id) REFERENCES seasons(id),
  CONSTRAINT fk_place_season_scores_place
    FOREIGN KEY (place_id) REFERENCES places(id)
);
Notes
ranking queries should use this table instead of recalculating from raw activities every time
the current ranking read API reads this table for dong, district, and city rankings
admin-triggered MVP recalculation deletes and rewrites rows for a season from `place_stats`
ranking weights are read from `system_policies`

6.3 place_ranking_history

Stores historical ranking and star result.

CREATE TABLE place_ranking_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_id BIGINT NOT NULL,
  season_id BIGINT NOT NULL,
  region_type VARCHAR(20) NOT NULL,
  region_ref_id BIGINT NOT NULL,
  rank_no INT NOT NULL,
  star_level INT NOT NULL,
  total_score DECIMAL(12,2) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_place_ranking_history_place
    FOREIGN KEY (place_id) REFERENCES places(id),
  CONSTRAINT fk_place_ranking_history_season
    FOREIGN KEY (season_id) REFERENCES seasons(id)
);
Notes
the entity is available for season finalization history
MVP read APIs do not populate this table yet

7. Mission Tables

These are not mandatory for the very first MVP, but are strongly recommended for future expansion.

7.1 missions
CREATE TABLE missions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mission_code VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  mission_type VARCHAR(30) NOT NULL,
  target_type VARCHAR(30) NOT NULL,
  target_count INT NOT NULL DEFAULT 1,
  reward_exp INT NOT NULL DEFAULT 0,
  reward_badge_code VARCHAR(50),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  start_at DATETIME NULL,
  end_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_missions_code (mission_code)
);

7.2 user_mission_progress
CREATE TABLE user_mission_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  mission_id BIGINT NOT NULL,
  current_count INT NOT NULL DEFAULT 0,
  is_completed BOOLEAN NOT NULL DEFAULT FALSE,
  completed_at DATETIME NULL,
  reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_user_mission_progress_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_mission_progress_mission
    FOREIGN KEY (mission_id) REFERENCES missions(id),
  UNIQUE KEY uk_user_mission_progress (user_id, mission_id)
);

8. Report and Admin Tables

8.1 reports

Stores reports against user, place, or comment.

CREATE TABLE reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_user_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  reason_code VARCHAR(50) NOT NULL,
  reason_text VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  review_note VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_reports_reporter
    FOREIGN KEY (reporter_user_id) REFERENCES users(id)
);

8.2 user_sanctions

Stores sanctions applied by admin.

CREATE TABLE user_sanctions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sanction_type VARCHAR(30) NOT NULL,
  reason VARCHAR(255),
  start_at DATETIME NOT NULL,
  end_at DATETIME NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_user_sanctions_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_sanctions_admin
    FOREIGN KEY (created_by) REFERENCES users(id)
);

8.3 admin_action_logs

Stores all important admin actions.

CREATE TABLE admin_action_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT NOT NULL,
  action_type VARCHAR(50) NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  before_value TEXT,
  after_value TEXT,
  memo VARCHAR(255),
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_admin_action_logs_admin
    FOREIGN KEY (admin_user_id) REFERENCES users(id)
);
Notes
every policy or moderation action should be logged here

9. Policy Tables

9.1 system_policies

Stores configurable business values.

CREATE TABLE system_policies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  policy_group VARCHAR(50) NOT NULL,
  policy_key VARCHAR(100) NOT NULL,
  policy_value VARCHAR(255) NOT NULL,
  value_type VARCHAR(20) NOT NULL,
  description VARCHAR(255),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  updated_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_system_policies_admin
    FOREIGN KEY (updated_by) REFERENCES users(id),
  UNIQUE KEY uk_system_policies_group_key (policy_group, policy_key)
);
Example Values
recommend.daily_limit = 20
visit.radius_meter = 70
visit.cooldown_hour = 24
region.change_cooldown_day = 7
region.registration_scope = DISTRICT
place.registration_limit = 5
ranking.recommend_weight = 1.0
ranking.visit_weight = 2.0
ranking.comment_weight = 0.5
Notes
this table is critical for admin-driven operation
do not hardcode these values in application logic
the region change API requires `policy_group = region` and `policy_key = change_cooldown_day`
the place creation API requires `policy_group = place` and `policy_key = registration_limit`
the place creation API requires `policy_group = region` and `policy_key = registration_scope`

9.2 Policy Seed Import

The backend supports an opt-in UTF-8 CSV import for default policy rows.

CSV columns:

policy_group,policy_key,policy_value,value_type,description

Rules:
- import is disabled by default
- set POLICY_SEED_ENABLED=true to run import on backend startup
- set POLICY_SEED_LOCATION to a classpath or file resource
- import inserts missing policies only and never overwrites admin-edited values
- the default classpath resource is `backend/src/main/resources/policy/policy-defaults.csv`

10. Logging Tables

10.1 user_action_logs

Stores important business events for monitoring and later abuse detection.

CREATE TABLE user_action_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  action_type VARCHAR(50) NOT NULL,
  target_type VARCHAR(20),
  target_id BIGINT,
  ip_address VARCHAR(64),
  user_agent VARCHAR(255),
  metadata_json JSON,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_user_action_logs_user
    FOREIGN KEY (user_id) REFERENCES users(id)
);
Notes
not all MVP builds need this immediately
highly useful for admin analysis and abuse tracing

11. Recommended MVP Table Set

The minimum useful MVP schema should include:

region_city
region_district
region_dong
users
user_auth
refresh_tokens
phone_verification_codes
user_region
user_trust
user_level
places
place_images
place_stats
recommendations
visits
comments
seasons
place_season_scores
reports
user_sanctions
admin_action_logs
system_policies
place_ranking_history

12. Recommended Future Expansion Tables

These can be added later if necessary:

place_audience_stats
place_tags
missions
user_mission_progress
user_level_history
user_action_logs

13. Important Index Recommendations

places
index on (region_dong_id, exposure_status)
index on (region_district_id, exposure_status)
index on (region_city_id, exposure_status)
recommendations
unique index on (user_id, place_id)
index on (place_id, status)
refresh_tokens
unique index on (token_hash)
index on (user_id, revoked, expires_at)
phone_verification_codes
index on (user_id, phone, verified, created_at)
visits
index on (place_id, created_at)
index on (user_id, place_id, created_at)
comments
unique index on (user_id, place_id)
index on (place_id, status)
place_season_scores
index on (season_id, region_type, region_ref_id, total_score)
reports
index on (status, target_type)
index on (reporter_user_id, created_at)

14. Final Notes

This schema is designed to balance:

MVP speed
policy flexibility
admin operability
future ranking and analytics expansion

The most important design choice is that the system is not just content-based.
It is also policy-driven and operator-controlled.

That is why:

policy tables
admin log tables
trust tables
aggregate score tables

are first-class parts of the schema, not optional extras.
