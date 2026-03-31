package com.codehaja.domain.progress.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.progress.dto.LectureItemEntryProgressDto;
import com.codehaja.domain.progress.dto.LectureProgressDto;
import com.codehaja.domain.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping("/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureProgressDto.Response>> saveLectureProgress(
            @PathVariable Long lectureId,
            @RequestBody LectureProgressDto.SaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        LectureProgressDto.Response response = progressService.saveLectureProgress(lectureId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Lecture progress saved successfully.", response));
    }

    @GetMapping("/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureProgressDto.Response>> getLectureProgress(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal UserDetails userDetails) {
        LectureProgressDto.Response response = progressService.getLectureProgress(lectureId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<LectureItemEntryProgressDto.Response>> saveEntryProgress(
            @PathVariable("entryId") Long lectureItemEntryId,
            @RequestBody LectureItemEntryProgressDto.SaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        LectureItemEntryProgressDto.Response response =
                progressService.saveEntryProgress(lectureItemEntryId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Entry progress saved successfully.", response));
    }

    @GetMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<LectureItemEntryProgressDto.Response>> getEntryProgress(
            @PathVariable("entryId") Long lectureItemEntryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        LectureItemEntryProgressDto.Response response =
                progressService.getEntryProgress(lectureItemEntryId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Integer>>> saveItemProgress(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        int xpGained = progressService.saveItemProgress(itemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Item progress saved.", java.util.Map.of("xpGained", xpGained)));
    }

    @GetMapping("/courses/{courseId}/completed-item-ids")
    public ResponseEntity<ApiResponse<java.util.List<Long>>> getCompletedItemIds(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        java.util.List<Long> ids = progressService.getCompletedItemIds(courseId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(ids));
    }

    @GetMapping("/courses/{courseId}/completed-item-count")
    public ResponseEntity<ApiResponse<Long>> getCompletedItemCount(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        long count = progressService.getCompletedItemCount(courseId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @GetMapping("/courses/{courseId}/lectures")
    public ResponseEntity<ApiResponse<LectureProgressDto.CourseSummary>> getCourseLectureProgress(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        LectureProgressDto.CourseSummary response =
                progressService.getCourseLectureProgress(courseId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
