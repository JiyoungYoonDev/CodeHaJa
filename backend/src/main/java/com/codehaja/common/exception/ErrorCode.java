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

    // ProblemBook
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK_404", "Book not found."),

    // CourseSection
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SECTION_404", "Section not found."),

    // Lecture
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "LECTURE_404", "Lecture not found."),

    // LectureItem
    LECTURE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "LECTURE_ITEM_404", "Lecture item not found."),

    // LectureItemEntry
    ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "ENTRY_404", "Lecture item entry not found.");


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
