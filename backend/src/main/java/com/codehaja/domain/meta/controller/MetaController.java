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
}