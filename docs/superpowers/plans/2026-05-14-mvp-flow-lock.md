# Honeytong MVP Flow Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lock Honeytong's MVP feature scope, add one minimal free-board community CRUD, and verify core user/admin flows before UI/UX polish.

**Architecture:** Keep community as a separate backend domain (`community`) with a single `community_posts` resource, owner-only mutation, public list/detail reads, and authentication-required write/update/delete. Keep the frontend community screen as one mobile-first route backed by a dedicated API module, while postponing comments, likes, categories, attachments, reports, and moderation to later expansion.

**Tech Stack:** Spring Boot 3.5, Java 21, Spring Data JPA, Spring Security JWT, MySQL/Flyway, React 19, TypeScript, Vite, Axios, Tailwind CSS.

---

## Scope Decisions

- Community scope is fixed to one free-board CRUD.
- MVP community includes post list, post detail, post creation, post update, and post deletion.
- Public users may read community posts.
- Authenticated active users may create posts.
- Only the author may update or delete their own posts.
- Phone verification and active-sanction guards should match other user-generated actions.
- Future expansion is explicitly out of scope: comments, likes, categories, image upload, post reports, admin moderation, pinned notices, search, pagination UI beyond a simple backend-friendly default.

## File Structure

Backend files:

- Create: `backend/src/main/java/com/honeytong/community/entity/CommunityPost.java`
- Create: `backend/src/main/java/com/honeytong/community/entity/CommunityPostStatus.java`
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostRequest.java`
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostResponse.java`
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostCreateResponse.java`
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostDeleteResponse.java`
- Create: `backend/src/main/java/com/honeytong/community/repository/CommunityPostRepository.java`
- Create: `backend/src/main/java/com/honeytong/community/service/CommunityPostService.java`
- Create: `backend/src/main/java/com/honeytong/community/controller/CommunityPostController.java`
- Modify: `backend/src/main/java/com/honeytong/auth/config/SecurityConfig.java`
- Modify: `backend/src/main/resources/db/migration/V1__baseline_schema.sql`
- Create: `backend/src/test/java/com/honeytong/community/service/CommunityPostServiceTest.java`

Frontend files:

- Create: `frontend/src/api/communityApi.ts`
- Modify: `frontend/src/pages/CommunityPage.tsx`
- Optional create if the page grows too large: `frontend/src/components/CommunityPostCard.tsx`

Verification files and docs:

- Modify: `README.md`
- Optional create: `docs/mvp-flow-checklist.md`

---

### Task 1: Backend Community Post Domain

**Files:**
- Create: `backend/src/main/java/com/honeytong/community/entity/CommunityPostStatus.java`
- Create: `backend/src/main/java/com/honeytong/community/entity/CommunityPost.java`
- Create: `backend/src/main/java/com/honeytong/community/repository/CommunityPostRepository.java`
- Modify: `backend/src/main/resources/db/migration/V1__baseline_schema.sql`

- [ ] **Step 1: Add the post status enum**

Create `CommunityPostStatus` with two values:

```java
package com.honeytong.community.entity;

public enum CommunityPostStatus {
    VISIBLE,
    DELETED
}
```

- [ ] **Step 2: Add the post entity**

Create `CommunityPost` with author, title, content, status, and soft delete timestamp. Follow `Comment` and `BaseTimeEntity` patterns.

```java
package com.honeytong.community.entity;

import com.honeytong.common.entity.BaseTimeEntity;
import com.honeytong.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_posts")
public class CommunityPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityPostStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected CommunityPost() {
    }

    public CommunityPost(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.status = CommunityPostStatus.VISIBLE;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public CommunityPostStatus getStatus() {
        return status;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isVisible() {
        return status == CommunityPostStatus.VISIBLE && deletedAt == null;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void delete() {
        this.status = CommunityPostStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Add the repository**

```java
package com.honeytong.community.repository;

import com.honeytong.community.entity.CommunityPost;
import com.honeytong.community.entity.CommunityPostStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findByStatusOrderByCreatedAtDesc(CommunityPostStatus status);

    List<CommunityPost> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CommunityPostStatus status);
}
```

- [ ] **Step 4: Add the migration table**

Append this table near other user-generated content tables in `V1__baseline_schema.sql`:

```sql
CREATE TABLE community_posts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  deleted_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_community_posts_status_created (status, created_at),
  KEY idx_community_posts_user_status_created (user_id, status, created_at),
  CONSTRAINT fk_community_posts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 5: Run backend tests**

Run:

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 2: Backend Community Service and Controller

**Files:**
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostRequest.java`
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostResponse.java`
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostCreateResponse.java`
- Create: `backend/src/main/java/com/honeytong/community/dto/CommunityPostDeleteResponse.java`
- Create: `backend/src/main/java/com/honeytong/community/service/CommunityPostService.java`
- Create: `backend/src/main/java/com/honeytong/community/controller/CommunityPostController.java`
- Modify: `backend/src/main/java/com/honeytong/auth/config/SecurityConfig.java`
- Create: `backend/src/test/java/com/honeytong/community/service/CommunityPostServiceTest.java`

- [ ] **Step 1: Add DTOs**

Use validation annotations to enforce MVP limits.

```java
package com.honeytong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityPostRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String content
) {
}
```

```java
package com.honeytong.community.dto;

import java.time.LocalDateTime;

public record CommunityPostResponse(
        Long postId,
        Long authorUserId,
        String authorNickname,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean mine
) {
}
```

```java
package com.honeytong.community.dto;

public record CommunityPostCreateResponse(Long postId) {
}
```

```java
package com.honeytong.community.dto;

public record CommunityPostDeleteResponse(Long postId, boolean deleted) {
}
```

- [ ] **Step 2: Add service behavior**

Implement:

- `createPost(userId, request)`
- `updatePost(userId, postId, request)`
- `deletePost(userId, postId)`
- `getPosts(viewerUserId)`
- `getPost(viewerUserId, postId)`
- `getMyPosts(userId)`

Validation rules:

- Normalize title/content with `trim()`.
- Blank title/content throws `ErrorCode.INVALID_REQUEST`.
- Unknown post throws `ErrorCode.RESOURCE_NOT_FOUND`.
- Deleted post behaves as not found.
- Non-author update/delete throws `ErrorCode.FORBIDDEN`.
- Inactive user throws `ErrorCode.FORBIDDEN`.

- [ ] **Step 3: Add controller endpoints**

Create endpoints:

```text
GET    /api/community/posts
GET    /api/community/posts/{postId}
POST   /api/community/posts
PATCH  /api/community/posts/{postId}
DELETE /api/community/posts/{postId}
GET    /api/users/me/community-posts
```

Use `@RequirePhoneVerified` and `@RequireNoActiveSanction` on create/update/delete.

- [ ] **Step 4: Permit public read endpoints**

Modify `SecurityConfig`:

```java
.requestMatchers(HttpMethod.GET, "/api/community/posts").permitAll()
.requestMatchers(HttpMethod.GET, "/api/community/posts/*").permitAll()
```

Keep mutations authenticated through `.anyRequest().authenticated()`.

- [ ] **Step 5: Add service tests**

Cover:

- Active user can create a post.
- Public list returns visible posts newest first.
- Author can update own post.
- Non-author cannot update.
- Author can delete own post.
- Deleted post is not returned.
- Inactive user cannot create.

- [ ] **Step 6: Run targeted and full backend tests**

Run:

```powershell
.\gradlew.bat test --tests "com.honeytong.community.service.CommunityPostServiceTest"
.\gradlew.bat test
```

Expected: both commands end with `BUILD SUCCESSFUL`.

---

### Task 3: Frontend Community CRUD

**Files:**
- Create: `frontend/src/api/communityApi.ts`
- Modify: `frontend/src/pages/CommunityPage.tsx`
- Optional create: `frontend/src/components/CommunityPostCard.tsx`

- [ ] **Step 1: Add community API module**

Create `communityApi.ts` with these exported types and functions:

```ts
import { api, type ApiResponse } from "./http";

export type CommunityPost = {
  postId: number;
  authorUserId: number;
  authorNickname: string;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  mine: boolean;
};

export type CommunityPostRequest = {
  title: string;
  content: string;
};

export const getCommunityPosts = async () => {
  const res = await api.get<ApiResponse<CommunityPost[]>>("/api/community/posts");
  return res.data.data;
};

export const getCommunityPost = async (postId: number) => {
  const res = await api.get<ApiResponse<CommunityPost>>(`/api/community/posts/${postId}`);
  return res.data.data;
};

export const createCommunityPost = async (request: CommunityPostRequest) => {
  const res = await api.post<ApiResponse<{ postId: number }>>("/api/community/posts", request);
  return res.data.data;
};

export const updateCommunityPost = async (postId: number, request: CommunityPostRequest) => {
  const res = await api.patch<ApiResponse<CommunityPost>>(`/api/community/posts/${postId}`, request);
  return res.data.data;
};

export const deleteCommunityPost = async (postId: number) => {
  const res = await api.delete<ApiResponse<{ postId: number; deleted: boolean }>>(`/api/community/posts/${postId}`);
  return res.data.data;
};
```

- [ ] **Step 2: Replace placeholder CommunityPage**

Implement one-page CRUD:

- Initial list load.
- Empty state.
- Inline create form.
- Selecting a post opens detail in the same route/page state.
- Author-only edit/delete controls when `mine` is true.
- Error messages use `getApiErrorMessage`.
- Unauthenticated write attempt shows the existing Korean login-required message from API error mapping.

- [ ] **Step 3: Keep UI small before polish**

Do not add categories, tabs, infinite scroll, image upload, emoji reactions, or nested comments. The page should feel complete but intentionally simple.

- [ ] **Step 4: Build frontend**

Run:

```powershell
npm.cmd run build
```

Expected: TypeScript and Vite build complete successfully.

---

### Task 4: MVP Core Flow Lock Checklist

**Files:**
- Create: `docs/mvp-flow-checklist.md`
- Modify: `README.md`

- [ ] **Step 1: Add checklist document**

Create a human-readable checklist with these sections:

```markdown
# Honeytong MVP Flow Checklist

## User Flows

- [ ] Signup creates an account.
- [ ] Login stores access and refresh tokens.
- [ ] Phone verification enables protected actions.
- [ ] Region verification sets the user's active neighborhood.
- [ ] Place list, search, nearby, and detail load.
- [ ] Authenticated user can create, edit, and delete own place.
- [ ] Authenticated user can recommend and cancel recommendation.
- [ ] Authenticated user can verify a visit within policy limits.
- [ ] Authenticated user can create, edit, and delete own place comment.
- [ ] Authenticated user can create, edit, and delete own community post.
- [ ] Authenticated user can report supported targets.
- [ ] My page shows profile, activity, registered places, and reports.

## Admin Flows

- [ ] Admin dashboard loads.
- [ ] Admin can review users.
- [ ] Admin can sanction users and adjust trust.
- [ ] Admin can moderate places.
- [ ] Admin can process reports.
- [ ] Admin can invalidate recommendations and visits.
- [ ] Super admin can update policies.
- [ ] Audit logs capture admin actions.

## Technical Gates

- [ ] Backend test suite passes.
- [ ] Frontend production build passes.
- [ ] Local backend starts with documented `.env`.
- [ ] Local frontend connects to backend through `VITE_API_BASE_URL`.
- [ ] Production profile requirements are documented.
```

- [ ] **Step 2: Update README status**

Replace the vague progress line with:

```markdown
현재 목표: MVP 기능 범위 잠금 및 핵심 사용자/관리자 흐름 검증.

커뮤니티는 1차 범위에서 자유게시판 CRUD 1개로 고정하고, 댓글/좋아요/카테고리/신고/관리자 모더레이션은 후속 확장으로 보류합니다.
```

- [ ] **Step 3: Run final verification**

Run:

```powershell
.\gradlew.bat test
npm.cmd run build
git status --short --branch
```

Expected:

- Backend: `BUILD SUCCESSFUL`
- Frontend: Vite build succeeds
- Git status shows only intentional source and documentation changes

---

## Implementation Order

1. Backend community domain and migration.
2. Backend service, controller, security, and tests.
3. Frontend community API and page.
4. MVP flow checklist and README status update.
5. Full verification.

## Self-Review

- Scope is focused on one minimal free-board CRUD and MVP flow lock.
- Expansion items are explicitly deferred.
- The plan follows existing backend controller/service/repository/entity patterns.
- The plan keeps frontend work inside the existing `/community` route.
- Verification commands are listed for backend and frontend.
