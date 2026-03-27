# CodeHaja Backend — API Test Coverage Report

**Generated:** 2026-03-24
**Test coverage status:** No controller or service tests exist. Only `DemoApplicationTests.java` (Spring context load) is present.

---

## Summary

| Controller | Endpoints | Inferred Test Cases | Tests Written | Coverage |
|---|---|---|---|---|
| AuthController | 7 | 32 | 0 | 0% |
| CourseController | 6 | 30 | 0 | 0% |
| CourseCategoryController | 5 | 22 | 0 | 0% |
| CourseSectionController | 6 | 30 | 0 | 0% |
| LectureController | 8 | 40 | 0 | 0% |
| LectureItemController | 6 | 30 | 0 | 0% |
| LectureItemEntryController | 6 | 30 | 0 | 0% |
| MetaController | 4 | 4 | 0 | 0% |
| AnonymousUserController | 1 | 4 | 0 | 0% |
| CodingDraftController | 2 | 12 | 0 | 0% |
| CodingSubmissionController | 3 | 14 | 0 | 0% |
| ProgressController | 4 | 18 | 0 | 0% |
| **TOTAL** | **58** | **266** | **0** | **0%** |

---

## Cross-Cutting Gaps and Risks

### 1. No Jakarta Bean Validation on any DTO
None of the DTO classes use `@NotNull`, `@NotBlank`, `@Size`, `@Email`, or `@Valid`. All validation is manual in service methods via `isBlank()` and null checks.

**Recommendation:** Add Bean Validation annotations and `@Valid` on controller `@RequestBody` parameters as a defense-in-depth layer.

### 2. Auth signup has no input validation
`SignupRequest` fields (email, password, name) are never validated for format, length, or presence. A user can register with an empty password or malformed email.

### 3. Category deletion with linked courses
`deleteCategory` does not check for courses referencing the category before deletion. If a FK constraint exists, this produces an unhandled `DataIntegrityViolationException` (500 error).

### 4. No auth/authorization on CMS endpoints
Course, section, lecture, lecture item, and entry CRUD endpoints have no role-based checks.

### 5. resetPassword performance issue
`AuthService.resetPassword` calls `userRepository.findAll()` and filters in Java to find the user by reset token. This should be a repository query: `findByPasswordResetToken(token)`.

### 6. Reorder endpoints accept partial lists
All 4 reorder endpoints allow submitting only a subset of items. Items not in the request retain their old sortOrder, which could create ordering gaps or duplicates.

---

## Priority Order for Test Implementation

| Priority | Domain | Reason |
|---|---|---|
| 1 | AuthController / AuthService | Security-critical, most complex error branches |
| 2 | CourseController / CourseService | Core domain, complex validation + section replacement |
| 3 | CourseCategoryController | Simple CRUD, good for establishing test patterns |
| 4 | CourseSectionController | Includes reorder + pagination |
| 5 | LectureController | Mirrors section + preview/publish toggles |
| 6 | LectureItemController | Follows established patterns |
| 7 | LectureItemEntryController | Follows established patterns |
| 8 | CodingDraftController + CodingSubmissionController | User-facing |
| 9 | ProgressController | User-facing |
| 10 | AnonymousUserController + MetaController | Trivial |

---

## 1. AuthController (`/api/auth`)

### POST /api/auth/signup

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid email, password, name | 200 | Returns MeResponse, sets cookies |
| 2 | Conflict | Email already exists | 409 | AUTH_EMAIL_ALREADY_EXISTS |
| 3 | Validation | Null/empty email | 400/500 | No explicit check — **potential bug** |
| 4 | Validation | Null/empty password | 400/500 | No explicit check — **potential bug** |
| 5 | Validation | Null/empty name | 200 | Succeeds with null name |

### POST /api/auth/login

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid credentials | 200 | Returns MeResponse, sets cookies |
| 2 | Not Found | Email not registered | 401 | AUTH_INVALID_CREDENTIALS |
| 3 | Auth | Wrong password | 401 | AUTH_INVALID_CREDENTIALS |
| 4 | Auth | Google-provider user | 401 | AUTH_INVALID_CREDENTIALS |
| 5 | Auth | Banned user | 403 | AUTH_ACCOUNT_BANNED |

### POST /api/auth/google

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid token, new user | 200 | Creates user |
| 2 | Happy | Valid token, existing user | 200 | Returns existing |
| 3 | Auth | Invalid/expired token | 401 | AUTH_INVALID_TOKEN |
| 4 | Auth | No email claim | 401 | AUTH_INVALID_TOKEN |

### POST /api/auth/logout

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Any request | 200 | Clears cookies |

### POST /api/auth/refresh

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid refresh token | 200 | Issues new tokens |
| 2 | Auth | No cookie | 401 | AUTH_INVALID_TOKEN |
| 3 | Auth | Expired JWT | 401 | AUTH_INVALID_TOKEN |
| 4 | Auth | Token mismatch | 401 | AUTH_INVALID_TOKEN |
| 5 | Auth | Expired refresh | 401 | AUTH_INVALID_TOKEN |
| 6 | Not Found | User gone | 401 | AUTH_INVALID_TOKEN |

### GET /api/auth/me

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Authenticated | 200 | Returns MeResponse |
| 2 | Auth | No auth | 401 | Unauthorized |
| 3 | Not Found | User not in DB | 401 | AUTH_UNAUTHORIZED |

### POST /api/auth/forgot-password & reset-password

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Existing email | 200 | Sets reset token |
| 2 | Happy | Non-existing email | 200 | Silent success |
| 3 | Happy | Valid reset token | 200 | Updates password |
| 4 | Auth | Invalid/expired token | 401 | AUTH_INVALID_TOKEN |

---

## 2. CourseController (`/api/courses`)

### POST /api/courses

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid request | 200 | Returns Response |
| 2 | Validation | Missing title | 400 | "Course title is required." |
| 3 | Validation | Missing difficulty | 400 | "Difficulty is required." |
| 4 | Validation | Missing status | 400 | "Course status is required." |
| 5 | Validation | Missing categoryId | 400 | "Category id is required." |
| 6 | Not Found | categoryId not in DB | 404 | CATEGORY_NOT_FOUND |
| 7 | Happy | Optional fields null | 200 | Defaults applied |

### GET /api/courses

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | No filters | 200 | All courses |
| 2 | Happy | Filter by categoryId/difficulty/status | 200 | Filtered |
| 3 | Not Found | Non-existent categoryId | 404 | CATEGORY_NOT_FOUND |
| 4 | Edge | No matches | 200 | Empty list |

### GET /api/courses/{courseId}

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid | 200 | DetailResponse |
| 2 | Not Found | Non-existent | 404 | COURSE_NOT_FOUND |

### PUT /api/courses/{courseId}

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid | 200 | Updated |
| 2 | Not Found | Non-existent | 404 | COURSE_NOT_FOUND |
| 3-6 | Validation | Missing required fields | 400 | Validation errors |
| 7 | Not Found | Non-existent categoryId | 404 | CATEGORY_NOT_FOUND |
| 8 | Happy | With sections | 200 | Sections replaced |
| 9 | Validation | Section blank title | 400 | Error |
| 10 | Validation | Section sortOrder < 1 | 400 | Error |

### PATCH /api/courses/{courseId}/status

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid | 200 | Updated |
| 2 | Not Found | Non-existent | 404 | COURSE_NOT_FOUND |
| 3 | Validation | Null status | 400 | Error |

### DELETE /api/courses/{courseId}

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid | 200 | Deleted |
| 2 | Not Found | Non-existent | 404 | COURSE_NOT_FOUND |

---

## 3. CourseCategoryController (`/api/course-categories`)

### POST /api/course-categories

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid name | 200 | Created |
| 2 | Validation | Empty name | 400 | Error |
| 3 | Conflict | Duplicate name | 409 | CATEGORY_ALREADY_EXISTS |

### GET /api/course-categories

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Exists | 200 | List with courseCount |
| 2 | Edge | Empty | 200 | Empty list |

### PUT /api/course-categories/{categoryId}

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | Valid | 200 | Updated |
| 2 | Not Found | Non-existent | 404 | CATEGORY_NOT_FOUND |
| 3 | Conflict | Duplicate name | 409 | CATEGORY_ALREADY_EXISTS |

### DELETE /api/course-categories/{categoryId}

| # | Category | Scenario | Expected Status | Expected Behavior |
|---|----------|----------|-----------------|-------------------|
| 1 | Happy | No linked courses | 200 | Deleted |
| 2 | Not Found | Non-existent | 404 | CATEGORY_NOT_FOUND |
| 3 | Edge | With linked courses | 500? | **FK constraint unhandled** |

---

## 4-7. Section / Lecture / LectureItem / LectureItemEntry Controllers

All follow the same CRUD + reorder pattern:
- **Create**: Happy path, parent not found, missing title, missing type, optional defaults
- **List**: Paginated with filters, parent not found, pagination edge cases
- **Get by ID**: Happy path, not found
- **Update**: Happy path, not found, missing required fields
- **Delete**: Happy path, not found
- **Reorder**: Happy path, parent not found, empty list, null fields, item not in parent, duplicates

---

## 8. MetaController — 4 simple GET endpoints returning static data

## 9. AnonymousUserController — POST /init with existing/new/null key scenarios

## 10. CodingDraftController — PUT (create/update draft) + GET (retrieve draft)

## 11. CodingSubmissionController — POST (submit) + GET by id + GET by entry

## 12. ProgressController — PUT/GET for lecture progress + PUT/GET for entry progress
