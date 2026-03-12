package com.codehaja.domain.meta.controller;

import com.codehaja.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MetaController {

    @GetMapping("/api/meta/page-sizes")
    public ResponseEntity<ApiResponse<List<Integer>>> getPageSizes() {
        return ResponseEntity.ok(ApiResponse.ok(List.of(10, 20, 50, 100)));
    }

    @GetMapping("/api/meta/section-sort-fields")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getSectionSortFields() {
        List<Map<String, String>> data = List.of(
                Map.of("value", "sortOrder", "label", "Sort Order"),
                Map.of("value", "title", "label", "Title"),
                Map.of("value", "createdAt", "label", "Created At"),
                Map.of("value", "updatedAt", "label", "Updated At")
        );

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/api/meta/lecture-entry-types")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, String>>>> getLectureEntryTypes() {
        List<java.util.Map<String, String>> data = List.of(
                java.util.Map.of("value", "MULTIPLE_CHOICE", "label", "Multiple Choice"),
                java.util.Map.of("value", "TRUE_FALSE", "label", "True / False"),
                java.util.Map.of("value", "SHORT_ANSWER", "label", "Short Answer"),
                java.util.Map.of("value", "CODING_CHALLENGE", "label", "Coding Challenge"),
                java.util.Map.of("value", "SQL_QUERY", "label", "SQL Query"),
                java.util.Map.of("value", "PROJECT_STEP", "label", "Project Step"),
                java.util.Map.of("value", "CHECKPOINT", "label", "Checkpoint"),
                java.util.Map.of("value", "TEST_QUESTION", "label", "Test Question")
        );

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/api/meta/access-levels")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, String>>>> getAccessLevels() {
        List<java.util.Map<String, String>> data = List.of(
                java.util.Map.of("value", "FREE", "label", "Free"),
                java.util.Map.of("value", "LOGIN_REQUIRED", "label", "Login Required"),
                java.util.Map.of("value", "PREMIUM", "label", "Premium")
        );

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}