-- Honeytong local UI smoke seed.
-- Purpose: local development/QA only. Do not run against production data.
-- Scope: public place cards, map markers, detail page, ranking page, and image fallback states.
-- Run from MySQL against the local `honey` schema:
--   mysql -u root -p honey < scripts/dev-seed-honeytong-ui-smoke.sql

USE honey;

SET @OLD_SQL_SAFE_UPDATES := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS dev_seed_place_ids;
DROP TEMPORARY TABLE IF EXISTS dev_seed_season_ids;

CREATE TEMPORARY TABLE dev_seed_place_ids (
  id BIGINT PRIMARY KEY
);

INSERT INTO dev_seed_place_ids (id)
SELECT id
FROM places
WHERE short_recommendation LIKE '%[DEV_SEED]%'
   OR feature_text LIKE '%[DEV_SEED]%'
   OR recommended_menu LIKE '%[DEV_SEED]%';

CREATE TEMPORARY TABLE dev_seed_season_ids (
  id BIGINT PRIMARY KEY
);

INSERT INTO dev_seed_season_ids (id)
SELECT id
FROM seasons
WHERE season_code = 'DEV-UI-SMOKE-2026-05';

DELETE FROM place_ranking_history
WHERE id IN (
  SELECT id
  FROM (
    SELECT prh.id
    FROM place_ranking_history prh
    JOIN dev_seed_place_ids dsp ON dsp.id = prh.place_id
    UNION
    SELECT prh.id
    FROM place_ranking_history prh
    JOIN dev_seed_season_ids dss ON dss.id = prh.season_id
  ) target_ids
);

DELETE FROM place_season_scores
WHERE id IN (
  SELECT id
  FROM (
    SELECT pss.id
    FROM place_season_scores pss
    JOIN dev_seed_place_ids dsp ON dsp.id = pss.place_id
    UNION
    SELECT pss.id
    FROM place_season_scores pss
    JOIN dev_seed_season_ids dss ON dss.id = pss.season_id
  ) target_ids
);

DELETE FROM comments
WHERE id IN (
  SELECT id
  FROM (
    SELECT c.id
    FROM comments c
    JOIN dev_seed_place_ids dsp ON dsp.id = c.place_id
  ) target_ids
);

DELETE FROM visits
WHERE id IN (
  SELECT id
  FROM (
    SELECT v.id
    FROM visits v
    JOIN dev_seed_place_ids dsp ON dsp.id = v.place_id
  ) target_ids
);

DELETE FROM recommendations
WHERE id IN (
  SELECT id
  FROM (
    SELECT r.id
    FROM recommendations r
    JOIN dev_seed_place_ids dsp ON dsp.id = r.place_id
  ) target_ids
);

DELETE FROM place_images
WHERE id IN (
  SELECT id
  FROM (
    SELECT pi.id
    FROM place_images pi
    JOIN dev_seed_place_ids dsp ON dsp.id = pi.place_id
  ) target_ids
);

DELETE FROM place_stats
WHERE place_id IN (
  SELECT id
  FROM dev_seed_place_ids
);

DELETE FROM places
WHERE id IN (
  SELECT id
  FROM dev_seed_place_ids
);

DELETE FROM seasons
WHERE id IN (
  SELECT id
  FROM dev_seed_season_ids
);

DROP TEMPORARY TABLE dev_seed_place_ids;
DROP TEMPORARY TABLE dev_seed_season_ids;

INSERT INTO users (
  nickname,
  email,
  phone,
  phone_verified,
  status,
  role,
  language_preference,
  marketing_agreed,
  created_at,
  updated_at
)
SELECT
  'UI 스모크 탐험가',
  'ui-smoke-dev@honeytong.local',
  '01000009999',
  TRUE,
  'ACTIVE',
  'USER',
  'ko',
  FALSE,
  NOW(6),
  NOW(6)
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE phone = '01000009999'
);

UPDATE users
SET
  nickname = 'UI 스모크 탐험가',
  phone_verified = TRUE,
  status = 'ACTIVE',
  language_preference = 'ko',
  updated_at = NOW(6)
WHERE phone = '01000009999';

SET @dev_user_id := (
  SELECT id FROM users WHERE phone = '01000009999' LIMIT 1
);

INSERT INTO user_trust (
  user_id,
  trust_score,
  trust_grade,
  recommend_weight,
  sanction_count,
  report_received_count,
  report_confirmed_count,
  abnormal_activity_score,
  phone_verified,
  region_verified,
  created_at,
  updated_at
)
SELECT
  @dev_user_id,
  12,
  'LOCAL_BEE',
  1.10,
  0,
  0,
  0,
  0,
  TRUE,
  TRUE,
  NOW(6),
  NOW(6)
WHERE NOT EXISTS (
  SELECT 1 FROM user_trust WHERE user_id = @dev_user_id
);

UPDATE user_trust
SET
  trust_score = 12,
  trust_grade = 'LOCAL_BEE',
  recommend_weight = 1.10,
  phone_verified = TRUE,
  region_verified = TRUE,
  updated_at = NOW(6)
WHERE user_id = @dev_user_id;

INSERT INTO user_level (
  user_id,
  level,
  exp,
  total_exp,
  title,
  avatar_stage,
  rank_score,
  created_at,
  updated_at
)
SELECT
  @dev_user_id,
  3,
  8,
  38,
  '동네 탐험가',
  'BEE_STAGE_3',
  0.00,
  NOW(6),
  NOW(6)
WHERE NOT EXISTS (
  SELECT 1 FROM user_level WHERE user_id = @dev_user_id
);

UPDATE user_level
SET
  level = 3,
  exp = 8,
  total_exp = 38,
  title = '동네 탐험가',
  avatar_stage = 'BEE_STAGE_3',
  updated_at = NOW(6)
WHERE user_id = @dev_user_id;

-- Prefer real Kakao administrative dong seed rows when they exist.
SET @city_id := (
  SELECT rc.id
  FROM region_city rc
  JOIN region_district rd ON rd.city_id = rc.id
  JOIN region_dong rg ON rg.district_id = rd.id
  WHERE rg.code = '1144066000'
  LIMIT 1
);

SET @district_id := (
  SELECT rd.id
  FROM region_district rd
  JOIN region_dong rg ON rg.district_id = rd.id
  WHERE rg.code = '1144066000'
  LIMIT 1
);

SET @dong_seogyo := (
  SELECT id FROM region_dong WHERE code = '1144066000' LIMIT 1
);

SET @dong_yeonnam := (
  SELECT id FROM region_dong WHERE code = '1144071000' LIMIT 1
);

SET @dong_mangwon := (
  SELECT id FROM region_dong WHERE code = '1144069000' LIMIT 1
);

-- Fallback rows keep the seed usable even before the full region CSV is imported.
INSERT INTO region_city (
  name_ko,
  name_en,
  code,
  created_at,
  updated_at
)
SELECT
  '서울특별시',
  'Seoul',
  'DEV-SEOUL',
  NOW(6),
  NOW(6)
WHERE @city_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM region_city WHERE code = 'DEV-SEOUL'
  );

SET @city_id := COALESCE(@city_id, (
  SELECT id FROM region_city WHERE code = 'DEV-SEOUL' LIMIT 1
));

INSERT INTO region_district (
  city_id,
  name_ko,
  name_en,
  code,
  created_at,
  updated_at
)
SELECT
  @city_id,
  '마포구',
  'Mapo-gu',
  'DEV-MAPO',
  NOW(6),
  NOW(6)
WHERE @district_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM region_district WHERE code = 'DEV-MAPO'
  );

SET @district_id := COALESCE(@district_id, (
  SELECT id FROM region_district WHERE code = 'DEV-MAPO' LIMIT 1
));

INSERT INTO region_dong (
  city_id,
  district_id,
  name_ko,
  name_en,
  code,
  created_at,
  updated_at
)
SELECT @city_id, @district_id, '서교동', 'Seogyo-dong', 'DEV-SEOGYO', NOW(6), NOW(6)
WHERE @dong_seogyo IS NULL
  AND NOT EXISTS (SELECT 1 FROM region_dong WHERE code = 'DEV-SEOGYO');

INSERT INTO region_dong (
  city_id,
  district_id,
  name_ko,
  name_en,
  code,
  created_at,
  updated_at
)
SELECT @city_id, @district_id, '연남동', 'Yeonnam-dong', 'DEV-YEONNAM', NOW(6), NOW(6)
WHERE @dong_yeonnam IS NULL
  AND NOT EXISTS (SELECT 1 FROM region_dong WHERE code = 'DEV-YEONNAM');

INSERT INTO region_dong (
  city_id,
  district_id,
  name_ko,
  name_en,
  code,
  created_at,
  updated_at
)
SELECT @city_id, @district_id, '망원제1동', 'Mangwon 1-dong', 'DEV-MANGWON', NOW(6), NOW(6)
WHERE @dong_mangwon IS NULL
  AND NOT EXISTS (SELECT 1 FROM region_dong WHERE code = 'DEV-MANGWON');

SET @dong_seogyo := COALESCE(@dong_seogyo, (
  SELECT id FROM region_dong WHERE code = 'DEV-SEOGYO' LIMIT 1
));

SET @dong_yeonnam := COALESCE(@dong_yeonnam, (
  SELECT id FROM region_dong WHERE code = 'DEV-YEONNAM' LIMIT 1
));

SET @dong_mangwon := COALESCE(@dong_mangwon, (
  SELECT id FROM region_dong WHERE code = 'DEV-MANGWON' LIMIT 1
));

INSERT INTO user_region (
  user_id,
  city_id,
  district_id,
  dong_id,
  is_primary,
  verified_at,
  changed_at,
  status,
  created_at,
  updated_at
)
SELECT
  @dev_user_id,
  @city_id,
  @district_id,
  @dong_seogyo,
  TRUE,
  NOW(6),
  NOW(6),
  'ACTIVE',
  NOW(6),
  NOW(6)
WHERE NOT EXISTS (
  SELECT 1 FROM user_region WHERE user_id = @dev_user_id AND status = 'ACTIVE'
);

INSERT INTO seasons (
  season_code,
  season_name,
  season_type,
  start_at,
  end_at,
  status,
  created_at,
  updated_at
)
VALUES (
  'DEV-UI-SMOKE-2026-05',
  '2026년 5월 UI 스모크 시즌',
  'MONTHLY',
  '2026-05-01 00:00:00.000000',
  '2026-05-31 23:59:59.000000',
  'ACTIVE',
  NOW(6),
  NOW(6)
);

SET @season_id := LAST_INSERT_ID();

CREATE TEMPORARY TABLE dev_seed_places (
  seed_no INT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  category_code VARCHAR(50) NOT NULL,
  dong_id BIGINT NOT NULL,
  address_road VARCHAR(255),
  address_jibun VARCHAR(255),
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  price_range_code VARCHAR(30),
  recommended_menu VARCHAR(255),
  short_recommendation VARCHAR(255) NOT NULL,
  feature_text VARCHAR(500),
  is_franchise BOOLEAN NOT NULL,
  current_star_level INT NOT NULL,
  current_flower_grade VARCHAR(30) NOT NULL,
  recommend_count INT NOT NULL,
  visit_count INT NOT NULL,
  comment_count INT NOT NULL,
  unique_user_count INT NOT NULL,
  score_total DECIMAL(12,2) NOT NULL,
  image_url VARCHAR(255)
);

INSERT INTO dev_seed_places VALUES
(1, '[테스트] 서교 골목 된장집', 'KOREAN', @dong_seogyo, '서울 마포구 와우산로 12', '서울 마포구 서교동 101-1', 37.5551000, 126.9234000, '10000_20000', '차돌 된장찌개', '[DEV_SEED] 진한 된장과 잘 지은 밥이 든든한 동네 밥집이에요.', '점심 피크에는 줄이 있지만 회전이 빠르고 혼밥 좌석도 편해서 카드 UI 기본형을 확인하기 좋습니다. [DEV_SEED]', FALSE, 3, 'GOLD', 42, 18, 7, 31, 81.50, 'https://images.unsplash.com/photo-1498654896293-37aacf113fd9?auto=format&fit=crop&w=900&q=80'),
(2, '[테스트] 연남 바삭 돈카츠 연구소', 'JAPANESE', @dong_yeonnam, '서울 마포구 동교로 45', '서울 마포구 연남동 202-3', 37.5610000, 126.9256000, '10000_20000', '등심 돈카츠', '[DEV_SEED] 바삭한 튀김옷과 촉촉한 고기가 균형 잡힌 돈카츠집이에요.', '이미지가 있는 카드, 높은 추천 수, 일식 카테고리 표시를 함께 확인하기 위한 개발용 데이터입니다. [DEV_SEED]', FALSE, 2, 'SILVER', 36, 14, 6, 24, 69.00, 'https://images.unsplash.com/photo-1617196034796-73dfa7b1fd56?auto=format&fit=crop&w=900&q=80'),
(3, '[테스트] 망원시장 긴 이름 떡볶이와 튀김 모둠집', 'SNACK', @dong_mangwon, '서울 마포구 포은로 71', '서울 마포구 망원동 403-9', 37.5568000, 126.9069000, '0_10000', '모둠 떡볶이와 꼬마김밥', '[DEV_SEED] 긴 이름과 모바일 카드 높이를 확인하기 위한 분식 맛집이에요.', '상호가 길어도 카드 제목, 상세 제목, 랭킹 리스트가 깨지지 않는지 보기 위한 테스트 데이터입니다. [DEV_SEED]', FALSE, 2, 'SILVER', 28, 22, 10, 33, 77.00, NULL),
(4, '[테스트] 합정 조용한 카페 볕을창가', 'CAFE', @dong_seogyo, '서울 마포구 독막로 8', '서울 마포구 서교동 150-2', 37.5489000, 126.9142000, '10000_20000', '버터 라떼', '[DEV_SEED] 이미지 없는 fallback과 카페 카테고리 확인용 데이터예요.', '대표 이미지가 없을 때 SpaceCard와 Detail 이미지 fallback 문구가 자연스럽게 보이는지 확인합니다. [DEV_SEED]', FALSE, 1, 'BRONZE', 18, 9, 5, 17, 42.50, NULL),
(5, '[테스트] 연남 향신료 국수집', 'KOREAN', @dong_yeonnam, '서울 마포구 성미산로 33', '서울 마포구 연남동 221-4', 37.5632000, 126.9215000, '10000_20000', '매운 들기름 국수', '[DEV_SEED] 향신료 향이 살아 있는 국수집으로 검색과 상세 설명 확인에 좋아요.', '설명 문구가 두 줄 이상일 때 카드 설명 clamp와 상세 본문 줄바꿈을 확인합니다. [DEV_SEED]', FALSE, 1, 'BRONZE', 21, 11, 3, 19, 45.50, 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=900&q=80'),
(6, '[테스트] 망원 아침 김밥', 'SNACK', @dong_mangwon, '서울 마포구 망원로 17', '서울 마포구 망원동 410-8', 37.5555000, 126.9041000, '0_10000', '참치김밥', '[DEV_SEED] 아침 일찍 열어 출근길에 들르기 좋은 김밥집이에요.', '낮은 가격대, 작은 점수, 일반적인 리스트 노출을 확인하는 개발용 데이터입니다. [DEV_SEED]', FALSE, 0, 'SEED', 9, 5, 2, 11, 20.00, 'https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=900&q=80'),
(7, '[테스트] 서교 숯불 갈비', 'KOREAN', @dong_seogyo, '서울 마포구 양화로 22', '서울 마포구 서교동 180-6', 37.5522000, 126.9189000, '20000_30000', '숯불 돼지갈비', '[DEV_SEED] 방문 인증과 추천 수가 모두 적당히 있는 고기집이에요.', '추천, 방문, 댓글 숫자가 모두 있는 카드와 상세 metric 표시를 확인하기 위한 데이터입니다. [DEV_SEED]', FALSE, 2, 'SILVER', 31, 16, 4, 23, 65.00, 'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=900&q=80'),
(8, '[테스트] 연남 긴 설명의 가지 비건 보울', 'CAFE', @dong_yeonnam, '서울 마포구 희우정로 11', '서울 마포구 연남동 240-7', 37.5651000, 126.9278000, '10000_20000', '구운 채소 보울', '[DEV_SEED] 카드 설명 말줄임을 확인하기 위해 일부러 긴 추천 문구를 넣은 비건 보울집이에요. 신선한 채소와 고소한 소스의 조합이 가볍게 어울려요.', '긴 설명이 모바일 상세에서 자연스럽게 줄바꿈되는지 확인하기 위한 테스트 데이터입니다. [DEV_SEED]', FALSE, 1, 'BRONZE', 16, 8, 6, 15, 38.00, NULL),
(9, '[테스트] 망원 구석 파스타집', 'JAPANESE', @dong_mangwon, '서울 마포구 월드컵로 31', '서울 마포구 망원동 429-1', 37.5574000, 126.9087000, '20000_30000', '바질 크림 파스타', '[DEV_SEED] 중간 점수와 대표 이미지가 있는 상세 화면 확인용 맛집이에요.', '이미지 있음, 점수 중간값, 상세 주소 표시를 확인하기 위한 개발용 데이터입니다. [DEV_SEED]', FALSE, 0, 'SEED', 12, 7, 2, 10, 28.00, 'https://images.unsplash.com/photo-1551183053-bf91a1d81141?auto=format&fit=crop&w=900&q=80'),
(10, '[테스트] 서교 디저트 꿀 케이크', 'CAFE', @dong_seogyo, '서울 마포구 잔다리로 9', '서울 마포구 서교동 120-5', 37.5507000, 126.9205000, '10000_20000', '꿀 케이크', '[DEV_SEED] 디저트성 카페 데이터와 낮은 댓글 상태를 확인하기 좋아요.', '랭킹 하위권, 실제 이미지, 짧은 설명을 함께 확인합니다. [DEV_SEED]', FALSE, 0, 'SEED', 7, 3, 1, 8, 14.50, 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=900&q=80');

INSERT INTO places (
  created_by,
  name,
  category_code,
  region_city_id,
  region_district_id,
  region_dong_id,
  address_road,
  address_jibun,
  latitude,
  longitude,
  price_range_code,
  recommended_menu,
  short_recommendation,
  feature_text,
  is_franchise,
  franchise_review_status,
  approval_status,
  exposure_status,
  current_star_level,
  current_flower_grade,
  ranking_excluded,
  created_at,
  updated_at
)
SELECT
  @dev_user_id,
  name,
  category_code,
  @city_id,
  @district_id,
  dong_id,
  address_road,
  address_jibun,
  latitude,
  longitude,
  price_range_code,
  recommended_menu,
  short_recommendation,
  feature_text,
  is_franchise,
  'PENDING',
  'APPROVED',
  'VISIBLE',
  current_star_level,
  current_flower_grade,
  FALSE,
  NOW(6),
  NOW(6)
FROM dev_seed_places
ORDER BY seed_no;

INSERT INTO place_stats (
  place_id,
  recommend_count,
  visit_count,
  comment_count,
  unique_user_count,
  score_total,
  manual_adjustment_score,
  recent_score,
  diversity_score,
  trust_weighted_score,
  last_activity_at,
  created_at,
  updated_at
)
SELECT
  p.id,
  s.recommend_count,
  s.visit_count,
  s.comment_count,
  s.unique_user_count,
  s.score_total,
  0.00,
  ROUND(s.score_total * 0.15, 2),
  ROUND(s.unique_user_count * 0.20, 2),
  s.score_total,
  NOW(6),
  NOW(6),
  NOW(6)
FROM places p
JOIN dev_seed_places s ON s.name = p.name
WHERE p.feature_text LIKE '%[DEV_SEED]%';

INSERT INTO place_images (
  place_id,
  image_url,
  sort_order,
  is_representative,
  uploaded_by,
  created_at,
  updated_at
)
SELECT
  p.id,
  s.image_url,
  0,
  TRUE,
  @dev_user_id,
  NOW(6),
  NOW(6)
FROM places p
JOIN dev_seed_places s ON s.name = p.name
WHERE p.feature_text LIKE '%[DEV_SEED]%'
  AND s.image_url IS NOT NULL;

INSERT INTO place_season_scores (
  season_id,
  place_id,
  region_type,
  region_ref_id,
  recommend_score,
  visit_score,
  comment_score,
  recent_bonus_score,
  diversity_bonus_score,
  trust_bonus_score,
  total_score,
  rank_no,
  star_level,
  created_at,
  updated_at
)
SELECT
  @season_id,
  p.id,
  region.region_type,
  region.region_ref_id,
  ROUND(s.recommend_count * 1.00, 2),
  ROUND(s.visit_count * 2.00, 2),
  ROUND(s.comment_count * 0.50, 2),
  ROUND(s.score_total * 0.05, 2),
  ROUND(s.unique_user_count * 0.10, 2),
  0.00,
  s.score_total,
  region.rank_no,
  region.star_level,
  NOW(6),
  NOW(6)
FROM places p
JOIN dev_seed_places s ON s.name = p.name
JOIN (
  SELECT 'DONG' AS region_type, @dong_seogyo AS region_ref_id, '[테스트] 서교 골목 된장집' AS place_name, 1 AS rank_no, 1 AS star_level
  UNION ALL SELECT 'DONG', @dong_seogyo, '[테스트] 서교 숯불 갈비', 2, 0
  UNION ALL SELECT 'DONG', @dong_seogyo, '[테스트] 합정 조용한 카페 볕을창가', 3, 0
  UNION ALL SELECT 'DONG', @dong_seogyo, '[테스트] 서교 디저트 꿀 케이크', 4, 0
  UNION ALL SELECT 'DONG', @dong_yeonnam, '[테스트] 연남 바삭 돈카츠 연구소', 1, 1
  UNION ALL SELECT 'DONG', @dong_yeonnam, '[테스트] 연남 향신료 국수집', 2, 0
  UNION ALL SELECT 'DONG', @dong_yeonnam, '[테스트] 연남 긴 설명의 가지 비건 보울', 3, 0
  UNION ALL SELECT 'DONG', @dong_mangwon, '[테스트] 망원시장 긴 이름 떡볶이와 튀김 모둠집', 1, 1
  UNION ALL SELECT 'DONG', @dong_mangwon, '[테스트] 망원 구석 파스타집', 2, 0
  UNION ALL SELECT 'DONG', @dong_mangwon, '[테스트] 망원 아침 김밥', 3, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 서교 골목 된장집', 1, 2
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 망원시장 긴 이름 떡볶이와 튀김 모둠집', 2, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 연남 바삭 돈카츠 연구소', 3, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 서교 숯불 갈비', 4, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 연남 향신료 국수집', 5, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 합정 조용한 카페 볕을창가', 6, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 연남 긴 설명의 가지 비건 보울', 7, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 망원 구석 파스타집', 8, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 망원 아침 김밥', 9, 0
  UNION ALL SELECT 'DISTRICT', @district_id, '[테스트] 서교 디저트 꿀 케이크', 10, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 서교 골목 된장집', 1, 3
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 망원시장 긴 이름 떡볶이와 튀김 모둠집', 2, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 연남 바삭 돈카츠 연구소', 3, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 서교 숯불 갈비', 4, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 연남 향신료 국수집', 5, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 합정 조용한 카페 볕을창가', 6, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 연남 긴 설명의 가지 비건 보울', 7, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 망원 구석 파스타집', 8, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 망원 아침 김밥', 9, 0
  UNION ALL SELECT 'CITY', @city_id, '[테스트] 서교 디저트 꿀 케이크', 10, 0
) region ON region.place_name = p.name
WHERE p.feature_text LIKE '%[DEV_SEED]%';

UPDATE places p
JOIN (
  SELECT '[테스트] 서교 골목 된장집' AS place_name, 3 AS star_level
  UNION ALL SELECT '[테스트] 망원시장 긴 이름 떡볶이와 튀김 모둠집', 1
  UNION ALL SELECT '[테스트] 연남 바삭 돈카츠 연구소', 1
  UNION ALL SELECT '[테스트] 서교 숯불 갈비', 0
  UNION ALL SELECT '[테스트] 연남 향신료 국수집', 0
  UNION ALL SELECT '[테스트] 합정 조용한 카페 볕을창가', 0
  UNION ALL SELECT '[테스트] 연남 긴 설명의 가지 비건 보울', 0
  UNION ALL SELECT '[테스트] 망원 구석 파스타집', 0
  UNION ALL SELECT '[테스트] 망원 아침 김밥', 0
  UNION ALL SELECT '[테스트] 서교 디저트 꿀 케이크', 0
) stars ON stars.place_name = p.name
SET
  p.current_star_level = stars.star_level,
  p.updated_at = NOW(6)
WHERE p.feature_text LIKE '%[DEV_SEED]%';

DROP TEMPORARY TABLE dev_seed_places;

COMMIT;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

SELECT
  'DEV UI smoke seed inserted' AS result,
  COUNT(*) AS place_count
FROM places
WHERE short_recommendation LIKE '%[DEV_SEED]%'
   OR feature_text LIKE '%[DEV_SEED]%'
   OR recommended_menu LIKE '%[DEV_SEED]%';

SELECT
  p.id,
  p.name,
  p.category_code,
  p.current_star_level,
  ps.recommend_count,
  ps.visit_count,
  ps.comment_count,
  pi.image_url AS representative_image_url
FROM places p
JOIN place_stats ps ON ps.place_id = p.id
LEFT JOIN place_images pi
  ON pi.place_id = p.id
 AND pi.is_representative = TRUE
WHERE p.short_recommendation LIKE '%[DEV_SEED]%'
   OR p.feature_text LIKE '%[DEV_SEED]%'
   OR p.recommended_menu LIKE '%[DEV_SEED]%'
ORDER BY p.id;

SELECT
  s.id,
  s.season_code,
  s.season_name,
  s.season_type,
  s.status,
  s.start_at,
  s.end_at
FROM seasons s
WHERE s.season_code = 'DEV-UI-SMOKE-2026-05';

SELECT
  s.season_code,
  s.season_name,
  pss.region_type,
  pss.region_ref_id,
  COUNT(*) AS ranking_item_count,
  MIN(pss.rank_no) AS first_rank,
  MAX(pss.rank_no) AS last_rank
FROM place_season_scores pss
JOIN seasons s ON s.id = pss.season_id
WHERE s.season_code = 'DEV-UI-SMOKE-2026-05'
GROUP BY
  s.season_code,
  s.season_name,
  pss.region_type,
  pss.region_ref_id
ORDER BY
  pss.region_type,
  pss.region_ref_id;
