-- V11: Add AI-based Place Tagging System schema

CREATE TABLE place_ai_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_id BIGINT NOT NULL,
    tag_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_place_ai_tags_place_id FOREIGN KEY (place_id) REFERENCES places(id) ON DELETE CASCADE,
    CONSTRAINT uq_place_id_tag_name UNIQUE (place_id, tag_name)
);

CREATE INDEX idx_place_ai_tags_place_id ON place_ai_tags(place_id);
