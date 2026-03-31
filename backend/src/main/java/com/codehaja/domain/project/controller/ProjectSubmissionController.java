package com.codehaja.domain.project.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.project.dto.ProjectSubmissionDto;
import com.codehaja.domain.project.service.ProjectSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project-submissions")
@RequiredArgsConstructor
public class ProjectSubmissionController {

    private final ProjectSubmissionService projectSubmissionService;

    @PostMapping("/{lectureItemId}")
    public ResponseEntity<ApiResponse<ProjectSubmissionDto.Response>> submit(
            @PathVariable Long lectureItemId,
            @RequestBody ProjectSubmissionDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectSubmissionDto.Response response =
                projectSubmissionService.submit(lectureItemId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Project submitted successfully.", response));
    }

    @GetMapping("/item/{lectureItemId}/latest")
    public ResponseEntity<ApiResponse<ProjectSubmissionDto.Response>> getLatest(
            @PathVariable Long lectureItemId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectSubmissionDto.Response response =
                projectSubmissionService.getLatestSubmission(lectureItemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
