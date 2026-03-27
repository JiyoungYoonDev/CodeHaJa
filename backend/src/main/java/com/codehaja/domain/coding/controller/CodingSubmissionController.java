package com.codehaja.domain.coding.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.coding.dto.CodingSubmissionDto;
import com.codehaja.domain.coding.service.CodingSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coding-submissions")
@RequiredArgsConstructor
public class CodingSubmissionController {

    private final CodingSubmissionService codingSubmissionService;

    @PostMapping("/{lectureItemId}")
    public ResponseEntity<ApiResponse<CodingSubmissionDto.Response>> submit(
            @PathVariable Long lectureItemId,
            @RequestBody CodingSubmissionDto.SubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CodingSubmissionDto.Response response = codingSubmissionService.submit(lectureItemId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Coding submission created successfully.", response));
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<CodingSubmissionDto.Response>> getSubmission(
            @PathVariable Long submissionId
    ) {
        CodingSubmissionDto.Response response = codingSubmissionService.getSubmission(submissionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/item/{lectureItemId}")
    public ResponseEntity<ApiResponse<List<CodingSubmissionDto.Response>>> getSubmissions(
            @PathVariable Long lectureItemId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<CodingSubmissionDto.Response> response =
                codingSubmissionService.getSubmissions(lectureItemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/course/{courseId}/passed-items")
    public ResponseEntity<ApiResponse<List<Long>>> getPassedItemIds(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<Long> ids = codingSubmissionService.getPassedItemIds(courseId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(ids));
    }

    @GetMapping("/item/{lectureItemId}/latest")
    public ResponseEntity<ApiResponse<CodingSubmissionDto.Response>> getLatestSubmission(
            @PathVariable Long lectureItemId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CodingSubmissionDto.Response response =
                codingSubmissionService.getLatestSubmission(lectureItemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
