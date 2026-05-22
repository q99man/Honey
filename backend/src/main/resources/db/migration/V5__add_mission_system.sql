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
  start_at DATETIME(6) NULL,
  end_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_missions_code (mission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_mission_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  mission_id BIGINT NOT NULL,
  current_count INT NOT NULL DEFAULT 0,
  is_completed BOOLEAN NOT NULL DEFAULT FALSE,
  completed_at DATETIME(6) NULL,
  reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_user_mission_progress_user
    FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_mission_progress_mission
    FOREIGN KEY (mission_id) REFERENCES missions(id),
  UNIQUE KEY uk_user_mission_progress (user_id, mission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Default Active Missions
INSERT INTO missions (mission_code, title, description, mission_type, target_type, target_count, reward_exp, created_at, updated_at)
VALUES 
('VISIT_3', '맛집 탐방가', '맛집을 3회 방문해 보세요.', 'ONCE', 'VISIT', 3, 50, NOW(6), NOW(6)),
('RECOMMEND_5', '추천 마스터', '맛집 추천을 5회 등록해 보세요.', 'ONCE', 'RECOMMEND', 5, 100, NOW(6), NOW(6)),
('COMMENT_3', '리뷰어의 첫걸음', '방문한 맛집에 한줄평을 3회 등록해 보세요.', 'ONCE', 'COMMENT', 3, 30, NOW(6), NOW(6));
