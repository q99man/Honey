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
