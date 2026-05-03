CREATE TABLE region_city (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name_ko VARCHAR(100) NOT NULL,
  name_en VARCHAR(100),
  name_ja VARCHAR(100),
  code VARCHAR(20),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_region_city_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE region_district (
  id BIGINT NOT NULL AUTO_INCREMENT,
  city_id BIGINT NOT NULL,
  name_ko VARCHAR(100) NOT NULL,
  name_en VARCHAR(100),
  name_ja VARCHAR(100),
  code VARCHAR(20),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_region_district_code (code),
  CONSTRAINT fk_region_district_city FOREIGN KEY (city_id) REFERENCES region_city (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE region_dong (
  id BIGINT NOT NULL AUTO_INCREMENT,
  city_id BIGINT NOT NULL,
  district_id BIGINT NOT NULL,
  name_ko VARCHAR(100) NOT NULL,
  name_en VARCHAR(100),
  name_ja VARCHAR(100),
  code VARCHAR(20),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_region_dong_code (code),
  CONSTRAINT fk_region_dong_city FOREIGN KEY (city_id) REFERENCES region_city (id),
  CONSTRAINT fk_region_dong_district FOREIGN KEY (district_id) REFERENCES region_district (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
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
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_auth (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider VARCHAR(20) NOT NULL,
  provider_user_id VARCHAR(200),
  email VARCHAR(150),
  password_hash VARCHAR(255),
  last_login_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_auth_provider (provider, provider_user_id),
  CONSTRAINT fk_user_auth_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(100) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  revoked_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_tokens_hash (token_hash),
  KEY idx_refresh_tokens_user_revoked_expires (user_id, revoked, expires_at),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE phone_verification_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  phone VARCHAR(30) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  verified_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_phone_verification_codes_user_phone_verified_created (user_id, phone, verified, created_at),
  CONSTRAINT fk_phone_verification_codes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_region (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  city_id BIGINT NOT NULL,
  district_id BIGINT NOT NULL,
  dong_id BIGINT NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT TRUE,
  verified_at DATETIME(6),
  changed_at DATETIME(6),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_user_region_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_region_city FOREIGN KEY (city_id) REFERENCES region_city (id),
  CONSTRAINT fk_user_region_district FOREIGN KEY (district_id) REFERENCES region_district (id),
  CONSTRAINT fk_user_region_dong FOREIGN KEY (dong_id) REFERENCES region_dong (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_trust (
  user_id BIGINT NOT NULL,
  trust_score INT NOT NULL DEFAULT 0,
  trust_grade VARCHAR(30) NOT NULL DEFAULT 'SEED_BEE',
  recommend_weight DECIMAL(4,2) NOT NULL DEFAULT 1.00,
  sanction_count INT NOT NULL DEFAULT 0,
  report_received_count INT NOT NULL DEFAULT 0,
  report_confirmed_count INT NOT NULL DEFAULT 0,
  abnormal_activity_score INT NOT NULL DEFAULT 0,
  phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
  region_verified BOOLEAN NOT NULL DEFAULT FALSE,
  last_evaluated_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_trust_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_level (
  user_id BIGINT NOT NULL,
  level INT NOT NULL DEFAULT 1,
  exp INT NOT NULL DEFAULT 0,
  total_exp INT NOT NULL DEFAULT 0,
  title VARCHAR(50),
  avatar_stage VARCHAR(50),
  rank_score DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_level_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_level_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  previous_level INT NOT NULL,
  new_level INT NOT NULL,
  changed_reason VARCHAR(100),
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_user_level_history_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE places (
  id BIGINT NOT NULL AUTO_INCREMENT,
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
  ranking_excluded BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6),
  PRIMARY KEY (id),
  KEY idx_places_dong_exposure (region_dong_id, exposure_status),
  KEY idx_places_district_exposure (region_district_id, exposure_status),
  KEY idx_places_city_exposure (region_city_id, exposure_status),
  CONSTRAINT fk_places_user FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_places_city FOREIGN KEY (region_city_id) REFERENCES region_city (id),
  CONSTRAINT fk_places_district FOREIGN KEY (region_district_id) REFERENCES region_district (id),
  CONSTRAINT fk_places_dong FOREIGN KEY (region_dong_id) REFERENCES region_dong (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE place_images (
  id BIGINT NOT NULL AUTO_INCREMENT,
  place_id BIGINT NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_representative BOOLEAN NOT NULL DEFAULT FALSE,
  uploaded_by BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_place_images_place FOREIGN KEY (place_id) REFERENCES places (id),
  CONSTRAINT fk_place_images_user FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE place_stats (
  place_id BIGINT NOT NULL,
  recommend_count INT NOT NULL DEFAULT 0,
  visit_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  unique_user_count INT NOT NULL DEFAULT 0,
  score_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  manual_adjustment_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  recent_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  diversity_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  trust_weighted_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  last_activity_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (place_id),
  CONSTRAINT fk_place_stats_place FOREIGN KEY (place_id) REFERENCES places (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recommendations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  recommend_weight DECIMAL(4,2) NOT NULL DEFAULT 1.00,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_recommendations_user_place (user_id, place_id),
  KEY idx_recommendations_place_status (place_id, status),
  CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_recommendations_place FOREIGN KEY (place_id) REFERENCES places (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE visits (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  distance_meter INT,
  image_url VARCHAR(255),
  is_valid BOOLEAN NOT NULL DEFAULT TRUE,
  valid_reason VARCHAR(100),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_visits_place_created (place_id, created_at),
  KEY idx_visits_user_place_created (user_id, place_id, created_at),
  CONSTRAINT fk_visits_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_visits_place FOREIGN KEY (place_id) REFERENCES places (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  content VARCHAR(300) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  report_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_comments_user_place (user_id, place_id),
  KEY idx_comments_place_status (place_id, status),
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_comments_place FOREIGN KEY (place_id) REFERENCES places (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seasons (
  id BIGINT NOT NULL AUTO_INCREMENT,
  season_code VARCHAR(30) NOT NULL,
  season_name VARCHAR(100) NOT NULL,
  season_type VARCHAR(20) NOT NULL,
  start_at DATETIME(6) NOT NULL,
  end_at DATETIME(6) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_seasons_code (season_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE place_season_scores (
  id BIGINT NOT NULL AUTO_INCREMENT,
  season_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  region_type VARCHAR(20) NOT NULL,
  region_ref_id BIGINT NOT NULL,
  recommend_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  visit_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  comment_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  recent_bonus_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  diversity_bonus_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  trust_bonus_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total_score DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  rank_no INT,
  star_level INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_place_season_scores_region_score (season_id, region_type, region_ref_id, total_score),
  CONSTRAINT fk_place_season_scores_season FOREIGN KEY (season_id) REFERENCES seasons (id),
  CONSTRAINT fk_place_season_scores_place FOREIGN KEY (place_id) REFERENCES places (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE place_ranking_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  place_id BIGINT NOT NULL,
  season_id BIGINT NOT NULL,
  region_type VARCHAR(20) NOT NULL,
  region_ref_id BIGINT NOT NULL,
  rank_no INT NOT NULL,
  star_level INT NOT NULL,
  total_score DECIMAL(12,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_place_ranking_history_place FOREIGN KEY (place_id) REFERENCES places (id),
  CONSTRAINT fk_place_ranking_history_season FOREIGN KEY (season_id) REFERENCES seasons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reports (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reporter_user_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  reason_code VARCHAR(50) NOT NULL,
  reason_text VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewed_by BIGINT,
  reviewed_at DATETIME(6),
  review_note VARCHAR(255),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_reports_status_target (status, target_type),
  KEY idx_reports_reporter_created (reporter_user_id, created_at),
  CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id),
  CONSTRAINT fk_reports_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_sanctions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sanction_type VARCHAR(30) NOT NULL,
  reason VARCHAR(255),
  start_at DATETIME(6) NOT NULL,
  end_at DATETIME(6),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_user_sanctions_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_sanctions_admin FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_action_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  admin_user_id BIGINT NOT NULL,
  action_type VARCHAR(50) NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  before_value TEXT,
  after_value TEXT,
  memo VARCHAR(255),
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_admin_action_logs_admin FOREIGN KEY (admin_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_policies (
  id BIGINT NOT NULL AUTO_INCREMENT,
  policy_group VARCHAR(50) NOT NULL,
  policy_key VARCHAR(100) NOT NULL,
  policy_value VARCHAR(255) NOT NULL,
  value_type VARCHAR(20) NOT NULL,
  description VARCHAR(255),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  updated_by BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_policies_group_key (policy_group, policy_key),
  CONSTRAINT fk_system_policies_admin FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_action_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  action_type VARCHAR(50) NOT NULL,
  target_type VARCHAR(20),
  target_id BIGINT,
  ip_address VARCHAR(64),
  user_agent VARCHAR(255),
  metadata_json JSON,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_user_action_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
