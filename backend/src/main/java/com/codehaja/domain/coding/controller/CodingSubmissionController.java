package com.codehaja.domain.coding.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.coding.dto.CodingSubmissionDto;
import com.codehaja.domain.coding.service.CodingSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coding-submissions")
@RequiredArgsConstructor
public class CodingSubmissionController {

    private final CodingSubmissionService codingSubmissionService;

    @PostMapping("/{lectureItemEntryId}")
    public ResponseEntity<ApiResponse<CodingSubmissionDto.Response>> submit(
            @PathVariable Long lectureItemEntryId,
            @RequestBody CodingSubmissionDto.SubmitRequest request
    ) {
        CodingSubmissionDto.Response response = codingSubmissionService.submit(lectureItemEntryId, request);
        return ResponseEntity.ok(ApiResponse.ok("Coding submission created successfully.", response));
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<CodingSubmissionDto.Response>> getSubmission(
            @PathVariable Long submissionId
    ) {
        CodingSubmissionDto.Response response = codingSubmissionService.getSubmission(submissionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/entry/{lectureItemEntryId}")
    public ResponseEntity<ApiResponse<List<CodingSubmissionDto.Response>>> getSubmissions(
            @PathVariable Long lectureItemEntryId,
            @RequestParam String anonymousUserKey
    ) {
        List<CodingSubmissionDto.Response> response =
                codingSubmissionService.getSubmissions(lectureItemEntryId, anonymousUserKey);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}