CREATE TABLE place_search_documents (
  place_id BIGINT NOT NULL,
  search_text VARCHAR(3000) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (place_id),
  KEY idx_place_search_documents_text (search_text(255)),
  CONSTRAINT fk_place_search_documents_place FOREIGN KEY (place_id) REFERENCES places (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO place_search_documents (
  place_id,
  search_text,
  created_at,
  updated_at
)
SELECT
  p.id,
  LOWER(CONCAT_WS(
    ' ',
    p.name,
    p.category_code,
    p.recommended_menu,
    p.short_recommendation,
    p.feature_text,
    p.address_road,
    p.address_jibun,
    city.name_ko,
    city.name_en,
    district.name_ko,
    district.name_en,
    dong.name_ko,
    dong.name_en
  )),
  CURRENT_TIMESTAMP(6),
  CURRENT_TIMESTAMP(6)
FROM places p
JOIN region_city city ON city.id = p.region_city_id
JOIN region_district district ON district.id = p.region_district_id
JOIN region_dong dong ON dong.id = p.region_dong_id
WHERE p.deleted_at IS NULL;
