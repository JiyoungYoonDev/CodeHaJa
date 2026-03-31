package com.codehaja.domain.lectureitem.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.common.api.PageResponse;
import com.codehaja.domain.lectureitem.dto.LectureItemDto;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import com.codehaja.domain.lectureitem.entity.ReviewStatus;
import com.codehaja.domain.lectureitem.service.LectureItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LectureItemController {

    private final LectureItemService lectureItemService;

    @PostMapping("/api/lectures/{lectureId}/items")
    public ResponseEntity<ApiResponse<LectureItemDto.DetailResponse>> createLectureItem(
            @PathVariable Long lectureId,
            @RequestBody LectureItemDto.CreateRequest request
    ) {
        LectureItemDto.DetailResponse response = lectureItemService.createLectureItem(lectureId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lecture item created successfully.", response));
    }

    @GetMapping("/api/lectures/{lectureId}/items")
    public ResponseEntity<ApiResponse<PageResponse<LectureItemDto.SummaryResponse>>> getLectureItems(
            @PathVariable Long lectureId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LectureItemType itemType,
            @RequestParam(required = false) ReviewStatus reviewStatus
    ) {
        Page<LectureItemDto.SummaryResponse> result =
                lectureItemService.getLectureItems(lectureId, page, size, keyword, itemType, reviewStatus);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/api/lecture-items/{lectureItemId}")
    public ResponseEntity<ApiResponse<LectureItemDto.DetailResponse>> getLectureItem(
            @PathVariable Long lectureItemId
    ) {
        LectureItemDto.DetailResponse response = lectureItemService.getLectureItem(lectureItemId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/api/lecture-items/{lectureItemId}")
    public ResponseEntity<ApiResponse<LectureItemDto.DetailResponse>> updateLectureItem(
            @PathVariable Long lectureItemId,
            @RequestBody LectureItemDto.UpdateRequest request
    ) {
        LectureItemDto.DetailResponse response = lectureItemService.updateLectureItem(lectureItemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lecture item updated successfully.", response));
    }

    @DeleteMapping("/api/lecture-items/{lectureItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteLectureItem(
            @PathVariable Long lectureItemId
    ) {
        lectureItemService.deleteLectureItem(lectureItemId);
        return ResponseEntity.ok(ApiResponse.ok("Lecture item deleted successfully."));
    }

    @PatchMapping("/api/lecture-items/{lectureItemId}/review-status")
    public ResponseEntity<ApiResponse<LectureItemDto.DetailResponse>> updateReviewStatus(
            @PathVariable Long lectureItemId,
            @RequestBody LectureItemDto.ReviewStatusRequest request
    ) {
        LectureItemDto.DetailResponse response = lectureItemService.updateReviewStatus(lectureItemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Review status updated.", response));
    }

    @PatchMapping("/api/lectures/{lectureId}/items/reorder")
    public ResponseEntity<ApiResponse<List<LectureItemDto.SummaryResponse>>> reorderLectureItems(
            @PathVariable Long lectureId,
            @RequestBody List<LectureItemDto.ReorderRequest> requests
    ) {
        List<LectureItemDto.SummaryResponse> response =
                lectureItemService.reorderLectureItems(lectureId, requests);

        return ResponseEntity.ok(ApiResponse.ok("Lecture items reordered successfully.", response));
    }
}