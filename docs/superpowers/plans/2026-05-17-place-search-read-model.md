# Place Search Read Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move public place search behind a dedicated search read model while preserving the existing `/api/places/search` response contract.

**Architecture:** Add a `place_search_documents` table keyed by `place_id`, keep it in sync from `PlaceService` on create/update/delete, and search the document text from `PlaceSearchDocumentRepository`. The first implementation keeps MVP-safe `LIKE` matching over a normalized text column, leaving the table boundary ready for MySQL full-text or N-gram indexes later.

**Tech Stack:** Spring Boot, Spring Data JPA, MySQL/Flyway, JUnit/Mockito.

---

### Task 1: Search Document Model

**Files:**
- Create: `backend/src/main/java/com/honeytong/place/entity/PlaceSearchDocument.java`
- Create: `backend/src/main/java/com/honeytong/place/repository/PlaceSearchDocumentRepository.java`
- Test: `backend/src/test/java/com/honeytong/place/service/PlaceSearchDocumentServiceTest.java`

- [x] Write a failing test that creates a document from a visible place and asserts normalized Korean/place text is searchable.
- [x] Implement `PlaceSearchDocument` and repository methods for visible document search.
- [x] Run the focused test and make it pass.

### Task 2: Sync Service and Place Search Integration

**Files:**
- Create: `backend/src/main/java/com/honeytong/place/service/PlaceSearchDocumentService.java`
- Modify: `backend/src/main/java/com/honeytong/place/service/PlaceService.java`
- Modify tests under `backend/src/test/java/com/honeytong/place/service`.

- [x] Write failing service tests for create/update/delete sync and `/api/places/search` using the read model.
- [x] Inject the sync service into `PlaceService`.
- [x] Save/update a search document after place create and update.
- [x] Delete the search document after logical place delete.
- [x] Change `searchPlaces` to read matching places from the search document repository.

### Task 3: Schema and Docs

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__add_place_search_documents.sql`
- Modify: `docs/db-schema.md`
- Modify: `docs/api-spec.md`
- Modify: `docs/architecture.md`
- Modify: `docs/decisions.md`
- Modify: `docs/tasks.md`
- Modify: `docs/progress.md`

- [x] Add the Flyway table with `place_id`, `search_text`, timestamps, FK, and a `search_text` prefix index for MVP filtering.
- [x] Document that full-text/N-gram remains a future optimization.
- [x] Record progress and the next recommended task.

### Task 4: Verification

**Files:**
- No new files.

- [ ] Run `.\gradlew.bat test` from `backend`.
- [ ] Run `git diff --check`.
- [ ] Review `git diff --stat` and summarize changed files.
