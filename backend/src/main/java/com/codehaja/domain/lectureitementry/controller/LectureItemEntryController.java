package com.codehaja.domain.lectureitementry.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.common.api.PageResponse;
import com.codehaja.domain.lectureitementry.dto.LectureItemEntryDto;
import com.codehaja.domain.lectureitementry.entity.AccessLevel;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntryType;
import com.codehaja.domain.lectureitementry.service.LectureItemEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LectureItemEntryController {

    private final LectureItemEntryService lectureItemEntryService;

    @PostMapping("/api/lecture-items/{lectureItemId}/entries")
    public ResponseEntity<ApiResponse<LectureItemEntryDto.DetailResponse>> createEntry(
            @PathVariable Long lectureItemId,
            @RequestBody LectureItemEntryDto.CreateRequest request
    ) {
        LectureItemEntryDto.DetailResponse response = lectureItemEntryService.createEntry(lectureItemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lecture item entry created successfully.", response));
    }

    @GetMapping("/api/lecture-items/{lectureItemId}/entries")
    public ResponseEntity<ApiResponse<PageResponse<LectureItemEntryDto.SummaryResponse>>> getEntries(
            @PathVariable Long lectureItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LectureItemEntryType entryType,
            @RequestParam(required = false) AccessLevel accessLevel,
            @RequestParam(required = false) Boolean isActive
    ) {
        Page<LectureItemEntryDto.SummaryResponse> result =
                lectureItemEntryService.getEntries(lectureItemId, page, size, keyword, entryType, accessLevel, isActive);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/api/lecture-item-entries/{entryId}")
    public ResponseEntity<ApiResponse<LectureItemEntryDto.DetailResponse>> getEntry(
            @PathVariable Long entryId
    ) {
        LectureItemEntryDto.DetailResponse response = lectureItemEntryService.getEntry(entryId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/api/lecture-item-entries/{entryId}")
    public ResponseEntity<ApiResponse<LectureItemEntryDto.DetailResponse>> updateEntry(
            @PathVariable Long entryId,
            @RequestBody LectureItemEntryDto.UpdateRequest request
    ) {
        LectureItemEntryDto.DetailResponse response = lectureItemEntryService.updateEntry(entryId, request);
        return ResponseEntity.ok(ApiResponse.ok("Lecture item entry updated successfully.", response));
    }

    @DeleteMapping("/api/lecture-item-entries/{entryId}")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(
            @PathVariable Long entryId
    ) {
        lectureItemEntryService.deleteEntry(entryId);
        return ResponseEntity.ok(ApiResponse.ok("Lecture item entry deleted successfully."));
    }

    @PatchMapping("/api/lecture-items/{lectureItemId}/entries/reorder")
    public ResponseEntity<ApiResponse<List<LectureItemEntryDto.SummaryResponse>>> reorderEntries(
            @PathVariable Long lectureItemId,
            @RequestBody List<LectureItemEntryDto.ReorderRequest> requests
    ) {
        List<LectureItemEntryDto.SummaryResponse> response =
                lectureItemEntryService.reorderEntries(lectureItemId, requests);

        return ResponseEntity.ok(ApiResponse.ok("Lecture item entries reordered successfully.", response));
    }
}