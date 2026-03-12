package com.codehaja.domain.category.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.category.dto.CourseCategoryDto;
import com.codehaja.domain.category.service.CourseCategoryService;

import lombok.RequiredArgsConstructor;

/*
GET    /api/course-categories
GET    /api/course-categories/{categoryId}
POST   /api/course-categories
PUT    /api/course-categories/{categoryId}
DELETE /api/course-categories/{categoryId}
*/
@RestController
@RequestMapping("/api/course-categories")
@RequiredArgsConstructor
public class CourseCategoryController {
    private final CourseCategoryService courseCategoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseCategoryDto.Response>> createCategory(@RequestBody CourseCategoryDto.Request request) {
        CourseCategoryDto.Response response = courseCategoryService.createCategory(request);

        return ResponseEntity.ok(ApiResponse.ok("Category created Successfully", response));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CourseCategoryDto.Response>> updateCategory(
        @PathVariable Long categoryId,
        @RequestBody CourseCategoryDto.Request request
    ) {
        CourseCategoryDto.Response response = courseCategoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", response));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
        @PathVariable Long categoryId
    ) {
        courseCategoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseCategoryDto.Response>>> getAllCategories() {
        List<CourseCategoryDto.Response> response = courseCategoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CourseCategoryDto.Response>> getCategory (
        @PathVariable Long categoryId
    ) {
        CourseCategoryDto.Response response = courseCategoryService.getCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
