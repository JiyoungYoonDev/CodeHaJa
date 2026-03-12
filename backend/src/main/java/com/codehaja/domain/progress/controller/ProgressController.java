package com.codehaja.domain.progress.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.progress.dto.LectureItemEntryProgressDto;
import com.codehaja.domain.progress.dto.LectureProgressDto;
import com.codehaja.domain.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping("/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureProgressDto.Response>> saveLectureProgress(
            @PathVariable Long lectureId,
            @RequestBody LectureProgressDto.SaveRequest request
    ) {
        LectureProgressDto.Response response = progressService.saveLectureProgress(lectureId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lecture progress saved successfully.", response));
    }

    @GetMapping("/lectures/{lectureId}")
    public ResponseEntity<ApiResponse<LectureProgressDto.Response>> getLectureProgress(
            @PathVariable Long lectureId,
            @RequestParam String anonymousUserKey
    ) {
        LectureProgressDto.Response response = progressService.getLectureProgress(lectureId, anonymousUserKey);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<LectureItemEntryProgressDto.Response>> saveEntryProgress(
            @PathVariable("entryId") Long lectureItemEntryId,
            @RequestBody LectureItemEntryProgressDto.SaveRequest request
    ) {
        LectureItemEntryProgressDto.Response response =
                progressService.saveEntryProgress(lectureItemEntryId, request);

        return ResponseEntity.ok(ApiResponse.ok("Entry progress saved successfully.", response));
    }

    @GetMapping("/entries/{entryId}")
    public ResponseEntity<ApiResponse<LectureItemEntryProgressDto.Response>> getEntryProgress(
            @PathVariable("entryId") Long lectureItemEntryId,
            @RequestParam String anonymousUserKey
    ) {
        LectureItemEntryProgressDto.Response response =
                progressService.getEntryProgress(lectureItemEntryId, anonymousUserKey);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}