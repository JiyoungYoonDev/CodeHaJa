package com.codehaja.domain.quiz.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.quiz.dto.QuizSubmissionDto;
import com.codehaja.domain.quiz.service.QuizSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz-submissions")
@RequiredArgsConstructor
public class QuizSubmissionController {

    private final QuizSubmissionService quizSubmissionService;

    @PostMapping("/{lectureItemId}")
    public ResponseEntity<ApiResponse<QuizSubmissionDto.Response>> submit(
            @PathVariable Long lectureItemId,
            @RequestBody QuizSubmissionDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        QuizSubmissionDto.Response response =
                quizSubmissionService.submit(lectureItemId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Quiz submitted successfully.", response));
    }

    @GetMapping("/item/{lectureItemId}/latest")
    public ResponseEntity<ApiResponse<QuizSubmissionDto.Response>> getLatest(
            @PathVariable Long lectureItemId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        QuizSubmissionDto.Response response =
                quizSubmissionService.getLatestSubmission(lectureItemId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
