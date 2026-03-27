package com.codehaja.domain.enrollment.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.enrollment.dto.CourseEnrollmentDto;
import com.codehaja.domain.enrollment.service.CourseEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class CourseEnrollmentController {

    private final CourseEnrollmentService enrollmentService;

    @PostMapping
    public ApiResponse<CourseEnrollmentDto.StatusResponse> enroll(
            @RequestBody CourseEnrollmentDto.EnrollRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(enrollmentService.enroll(request.getCourseId(), userDetails.getUsername()));
    }

    @GetMapping("/status")
    public ApiResponse<CourseEnrollmentDto.StatusResponse> getStatus(
            @RequestParam Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.success(enrollmentService.getStatus(courseId, userDetails.getUsername()));
    }
}
