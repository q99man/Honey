# Implementation Plan - Mission System

This implementation plan details the design and deployment steps for the **Mission System** in Honeytong. This system tracks user activities, updates progress, rewards EXP when completed, grants badges, and allows users to view active missions, progress, and claim rewards.

---

## User Review Required

### 1. Mission Types and Reset Rules
We support two types of missions:
- `ONCE`: The user can complete and claim this mission only once. Once claimed, `rewardClaimed` becomes `true`.
- `REPEATABLE`: The user can complete this mission multiple times. Upon claiming the reward, progress is reset:
  - Option A: Reset `currentCount` back to `0`.
  - Option B: Subtract the `targetCount` from `currentCount` (preserving overflow progress).
  - In both options, `isCompleted` is reset to `false` and `rewardClaimed` to `false`, allowing progress to resume. We will implement **Option A (reset to 0)** as default, but support preserving overflow (Option B) if there is surplus progress.

### 2. Seed Data for Default Missions
We will seed the database with the following active missions:
- `VISIT_3`: "맛집 탐방가" - 맛집을 3회 방문해 보세요. (Reward: 50 EXP, Target type: `VISIT`)
- `RECOMMEND_5`: "추천 마스터" - 맛집 추천을 5회 등록해 보세요. (Reward: 100 EXP, Target type: `RECOMMEND`)
- `COMMENT_3`: "리뷰어의 첫걸음" - 방문한 맛집에 한줄평을 3회 등록해 보세요. (Reward: 30 EXP, Target type: `COMMENT`)

### 3. Korean Exceptions / User-Facing Messages
All errors for claim validation will return user-friendly Korean messages:
- If mission progress is not complete: `"미션이 아직 완료되지 않았습니다."`
- If reward is already claimed: `"이미 보상을 수령한 미션입니다."`

---

## Proposed Changes

### Database Migration

#### [NEW] [V5__add_mission_system.sql](file:///c:/SpringWork/Honey/backend/src/main/resources/db/migration/V5__add_mission_system.sql)
Creates the `missions` and `user_mission_progress` tables as specified in `db-schema.md` and seeds default active missions.

```sql
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

-- Seed Default Active Missions
INSERT INTO missions (mission_code, title, description, mission_type, target_type, target_count, reward_exp, created_at, updated_at)
VALUES 
('VISIT_3', '맛집 탐방가', '맛집을 3회 방문해 보세요.', 'ONCE', 'VISIT', 3, 50, NOW(), NOW()),
('RECOMMEND_5', '추천 마스터', '맛집 추천을 5회 등록해 보세요.', 'ONCE', 'RECOMMEND', 5, 100, NOW(), NOW()),
('COMMENT_3', '리뷰어의 첫걸음', '방문한 맛집에 한줄평을 3회 등록해 보세요.', 'ONCE', 'COMMENT', 3, 30, NOW(), NOW());
```

---

### User Growth Refactoring

#### [MODIFY] [UserGrowthService.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/user/service/UserGrowthService.java)
- Add `rewardExp(Long userId, int exp, String reason)` method.
- Update `applyLevelUp` to accept a dynamic reason string instead of a hardcoded `"VALID_VISIT"` string.

---

### Mission Domain Implementation

#### [NEW] [MissionType.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/entity/MissionType.java)
Enum for `ONCE` and `REPEATABLE`.

#### [NEW] [MissionTargetType.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/entity/MissionTargetType.java)
Enum for `VISIT`, `RECOMMEND`, `COMMENT`, `PLACE_REGISTER`.

#### [NEW] [Mission.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/entity/Mission.java)
JPA entity mapping for `missions` table.

#### [NEW] [UserMissionProgress.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/entity/UserMissionProgress.java)
JPA entity mapping for `user_mission_progress` table.

#### [NEW] [MissionRepository.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/repository/MissionRepository.java)
Repository for `Mission`. Includes method to query active missions.

#### [NEW] [UserMissionProgressRepository.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/repository/UserMissionProgressRepository.java)
Repository for `UserMissionProgress`.

#### [NEW] [MissionResponse.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/dto/MissionResponse.java)
DTO for public list of active missions.

#### [NEW] [UserMissionProgressResponse.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/dto/UserMissionProgressResponse.java)
DTO for user's own progress on missions.

#### [NEW] [MissionClaimResponse.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/dto/MissionClaimResponse.java)
DTO representing reward claim results (exp gained, reward badge code).

#### [NEW] [MissionService.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/service/MissionService.java)
Implements:
- `List<MissionResponse> getActiveMissions()`
- `List<UserMissionProgressResponse> getUserMissionProgresses(Long userId)`
- `void trackProgress(Long userId, MissionTargetType targetType)`: Called from domain hooks to increment progress.
- `MissionClaimResponse claimReward(Long userId, Long missionId)`: Validates progress, triggers `UserGrowthService.rewardExp` with reason `"MISSION_COMPLETE_" + missionCode`, handles reset rules for `REPEATABLE` missions, and marks `ONCE` missions as claimed.

#### [NEW] [MissionController.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/mission/controller/MissionController.java)
Exposes endpoints:
- `GET /api/missions` (public)
- `GET /api/users/me/missions` (authenticated)
- `POST /api/users/me/missions/{missionId}/claim` (authenticated)

---

### Service Integrations (Domain Hooks)

#### [MODIFY] [RecommendationService.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/recommendation/service/RecommendationService.java)
- Inject `MissionService`.
- Call `missionService.trackProgress(userId, MissionTargetType.RECOMMEND)` inside `recommend` after successfully creating a recommendation.

#### [MODIFY] [VisitService.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/visit/service/VisitService.java)
- Inject `MissionService`.
- Call `missionService.trackProgress(userId, MissionTargetType.VISIT)` inside `verifyVisit` after verifying visit.

#### [MODIFY] [CommentService.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/comment/service/CommentService.java)
- Inject `MissionService`.
- Call `missionService.trackProgress(userId, MissionTargetType.COMMENT)` inside `createComment` after creating comment.

#### [MODIFY] [PlaceService.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/place/service/PlaceService.java)
- Inject `MissionService`.
- Call `missionService.trackProgress(userId, MissionTargetType.PLACE_REGISTER)` inside `createPlace` after creating place.

#### [MODIFY] [SecurityConfig.java](file:///c:/SpringWork/Honey/backend/src/main/java/com/honeytong/auth/config/SecurityConfig.java)
- Expose `GET /api/missions` to public request matchers.
