package com.codehaja.domain.checkpoint.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.checkpoint.dto.CheckpointSubmissionDto;
import com.codehaja.domain.checkpoint.service.CheckpointSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkpoint-submissions")
@RequiredArgsConstructor
public class CheckpointSubmissionController {

    private final CheckpointSubmissionService checkpointSubmissionService;

    @PostMapping("/{lectureItemId}")
    public ResponseEntity<ApiResponse<CheckpointSubmissionDto.Response>> submit(
            @PathVariable Long lectureItemId,
            @RequestBody CheckpointSubmissionDto.SubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CheckpointSubmissionDto.Response response =
                checkpointSubmissionService.submit(lectureItemId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Checkpoint submitted.", response));
    }

    @GetMapping("/item/{lectureItemId}")
    public ResponseEntity<ApiResponse<CheckpointSubmissionDto.ItemSubmissions>> getSubmissions(
            @PathVariable Long lectureItemId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CheckpointSubmissionDto.ItemSubmissions response =
                checkpointSubmissionService.getSubmissions(lectureItemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
