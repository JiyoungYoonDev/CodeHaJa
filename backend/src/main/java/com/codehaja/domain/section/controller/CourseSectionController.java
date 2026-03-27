package com.codehaja.domain.section.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.common.api.PageResponse;
import com.codehaja.domain.section.dto.CourseSectionDto;
import com.codehaja.domain.section.service.CourseSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/* 
POST /api/courses/{courseId}/sections ok
GET /api/courses/{courseId}/sections ok
GET /api/courses/3/sections?page=0&size=10&keyword=java
GET /api/course-sections/{sectionId} ok
PUT /api/course-sections/{sectionId} ok
DELETE /api/course-sections/{sectionId}
PATCH /api/courses/{courseId}/sections/reorder
*/
@RestController
@RequiredArgsConstructor
public class CourseSectionController {

    private final CourseSectionService courseSectionService;

    @PostMapping("/api/courses/{courseId}/sections")
    public ResponseEntity<ApiResponse<CourseSectionDto.DetailResponse>> createSection(
            @PathVariable Long courseId,
            @RequestBody CourseSectionDto.CreateRequest request
    ) {
        CourseSectionDto.DetailResponse response = courseSectionService.createSection(courseId, request);
        return ResponseEntity.ok(ApiResponse.ok("Section created successfully.", response));
    }

    @GetMapping("/api/courses/{courseId}/sections")
    public ResponseEntity<ApiResponse<PageResponse<CourseSectionDto.SummaryResponse>>> getSections(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        Page<CourseSectionDto.SummaryResponse> result =
                courseSectionService.getSections(courseId, page, size, keyword);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/api/course-sections/{sectionId}")
    public ResponseEntity<ApiResponse<CourseSectionDto.DetailResponse>> getSection(
            @PathVariable Long sectionId
    ) {
        CourseSectionDto.DetailResponse response = courseSectionService.getSection(sectionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/courses/{courseId}/sections/{sectionId}")
    public ResponseEntity<ApiResponse<CourseSectionDto.DetailResponse>> getSectionByCourse(
            @PathVariable Long courseId,
            @PathVariable Long sectionId
    ) {
        CourseSectionDto.DetailResponse response = courseSectionService.getSection(sectionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/api/course-sections/{sectionId}")
    public ResponseEntity<ApiResponse<CourseSectionDto.DetailResponse>> updateSection(
            @PathVariable Long sectionId,
            @RequestBody CourseSectionDto.UpdateRequest request
    ) {
        CourseSectionDto.DetailResponse response = courseSectionService.updateSection(sectionId, request);
        return ResponseEntity.ok(ApiResponse.ok("Section updated successfully.", response));
    }

    @DeleteMapping("/api/course-sections/{sectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable Long sectionId
    ) {
        courseSectionService.deleteSection(sectionId);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted successfully."));
    }

    @PatchMapping("/api/courses/{courseId}/sections/reorder")
    public ResponseEntity<ApiResponse<List<CourseSectionDto.SummaryResponse>>> reorderSections(
            @PathVariable Long courseId,
            @RequestBody List<CourseSectionDto.ReorderRequest> requests
    ) {
        List<CourseSectionDto.SummaryResponse> response =
                courseSectionService.reorderSections(courseId, requests);

        return ResponseEntity.ok(ApiResponse.ok("Sections reordered successfully.", response));
    }
}