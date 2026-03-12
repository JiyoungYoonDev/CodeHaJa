package com.codehaja.domain.coding.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.coding.dto.CodingDraftDto;
import com.codehaja.domain.coding.service.CodingDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coding-drafts")
@RequiredArgsConstructor
public class CodingDraftController {

    private final CodingDraftService codingDraftService;

    @PutMapping("/{lectureItemEntryId}")
    public ResponseEntity<ApiResponse<CodingDraftDto.Response>> saveDraft(
            @PathVariable Long lectureItemEntryId,
            @RequestBody CodingDraftDto.SaveRequest request
    ) {
        CodingDraftDto.Response response = codingDraftService.saveDraft(lectureItemEntryId, request);
        return ResponseEntity.ok(ApiResponse.ok("Coding draft saved successfully.", response));
    }

    @GetMapping("/{lectureItemEntryId}")
    public ResponseEntity<ApiResponse<CodingDraftDto.Response>> getDraft(
            @PathVariable Long lectureItemEntryId,
            @RequestParam String anonymousUserKey
    ) {
        CodingDraftDto.Response response = codingDraftService.getDraft(lectureItemEntryId, anonymousUserKey);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}