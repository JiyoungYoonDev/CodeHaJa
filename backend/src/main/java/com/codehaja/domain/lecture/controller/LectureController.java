package com.codehaja.domain.lecture.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.common.api.PageResponse;
import com.codehaja.domain.lecture.dto.LectureDto;
import com.codehaja.domain.lecture.service.LectureService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    @PostMapping("/api/course-sections/{courseSectionId}/lectures")
    public ResponseEntity<ApiResponse<LectureDto.DetailResponse>> createLecture(
            @PathVariable Long courseSectionId,
            @RequestBody LectureDto.CreateRequest request
    ) {
        LectureDto.DetailResponse response = lectureService.createLecture(courseSectionId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lecture created successfully.", response));
    }

    @GetMapping("/api/course-sections/{courseSectionId}/lectures")
    public ResponseEntity<ApiResponse<PageResponse<LectureDto.SummaryResponse>>> getLectures(
            @PathVariable Long courseSectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) Boolean isPreview
    ) {
        Page<LectureDto.SummaryResponse> result =
                lectureService.getLectures(courseSectionId, page, size, keyword, isPublished, isPreview);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/api/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureDto.DetailResponse>> getLecture(
            @PathVariable Long lectureId
    ) {
        LectureDto.DetailResponse response = lectureService.getLecture(lectureId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/course-sections/{courseSectionId}/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureDto.DetailResponse>> getLectureBySection(
            @PathVariable Long courseSectionId,
            @PathVariable Long lectureId
    ) {
        LectureDto.DetailResponse response = lectureService.getLecture(lectureId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/api/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureDto.DetailResponse>> updateLecture(
            @PathVariable Long lectureId,
            @RequestBody LectureDto.UpdateRequest request
    ) {
        LectureDto.DetailResponse response = lectureService.updateLecture(lectureId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lecture updated successfully.", response));
    }

    @PatchMapping("/api/lectures/{lectureId}/preview")
    public ResponseEntity<ApiResponse<LectureDto.DetailResponse>> updatePreview(
            @PathVariable Long lectureId,
            @RequestBody LectureDto.PreviewRequest request
    ) {
        LectureDto.DetailResponse response = lectureService.updatePreview(lectureId, request.getIsPreview());
        return ResponseEntity.ok(ApiResponse.ok("Lecture preview updated successfully.", response));
    }

    @PatchMapping("/api/lectures/{lectureId}/publish")
    public ResponseEntity<ApiResponse<LectureDto.DetailResponse>> updatePublish(
            @PathVariable Long lectureId,
            @RequestBody LectureDto.PublishRequest request
    ) {
        LectureDto.DetailResponse response = lectureService.updatePublish(lectureId, request.getIsPublished());
        return ResponseEntity.ok(ApiResponse.ok("Lecture publish status updated successfully.", response));
    }

    @DeleteMapping("/api/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<Void>> deleteLecture(
            @PathVariable Long lectureId
    ) {
        lectureService.deleteLecture(lectureId);
        return ResponseEntity.ok(ApiResponse.ok("Lecture deleted successfully."));
    }

    @PatchMapping("/api/course-sections/{courseSectionId}/lectures/reorder")
    public ResponseEntity<ApiResponse<List<LectureDto.SummaryResponse>>> reorderLectures(
            @PathVariable Long courseSectionId,
            @RequestBody List<LectureDto.ReorderRequest> requests
    ) {
        List<LectureDto.SummaryResponse> response =
                lectureService.reorderLectures(courseSectionId, requests);

        return ResponseEntity.ok(ApiResponse.ok("Lectures reordered successfully.", response));
    }
}