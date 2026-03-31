package com.codehaja.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "Invalid input."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "Internal server error."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "Resource not found."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "COMMON_409", "Resource already exists."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "Method not allowed."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_404", "Category not found."),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "CATEGORY_409", "Category already exists."),
    CATEGORY_HAS_COURSES(HttpStatus.CONFLICT, "CATEGORY_409_LINKED", "Category has linked courses."),

    // Course
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_404", "Course not found."),

    // CourseSection
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SECTION_404", "Section not found."),
    SECTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "SECTION_409", "Section already exists."),
    SECTION_REORDER_INVALID(HttpStatus.BAD_REQUEST, "SECTION_400", "Invalid section reorder."),
    
    // Lecture
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "LECTURE_404", "Lecture not found."),
    LECTURE_REORDER_INVALID(HttpStatus.BAD_REQUEST, "LECTURE_400", "Invalid lecture reorder."),

    // LectureItem
    LECTURE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "LECTURE_ITEM_404", "Lecture item not found."),
    LECTURE_ITEM_REORDER_INVALID(HttpStatus.BAD_REQUEST, "LECTURE_ITEM_400", "Invalid lecture item reorder."),

    // LectureItemEntry
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "ENTRY_404", "Lecture item entry not found."),
    ENTRY_REORDER_INVALID(HttpStatus.BAD_REQUEST, "ENTRY_400", "Invalid lecture item entry reorder."),

    // Anonymous
    ANONYMOUS_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ANON_404", "Anonymous user not found."),
    
    // Coding
    CODING_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "CODING_404", "Coding draft not found."),
    CODING_SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CODING_404", "Coding submission not found."),

    // Gamification
    HEART_EMPTY(HttpStatus.valueOf(422), "HEART_422", "하트가 없습니다. 4시간 후 충전되거나 이전 강의를 복습하면 충전됩니다."),

    // Auth
    AUTH_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_409", "Email already exists."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401", "Invalid email or password."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_TOKEN", "Invalid or expired token."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401_UNAUTH", "Authentication required."),
    AUTH_ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "AUTH_403", "Account is banned."),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "AUTH_403_LOCKED", "Account is temporarily locked.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
