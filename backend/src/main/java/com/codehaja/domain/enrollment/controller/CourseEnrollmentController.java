package com.codehaja.domain.enrollment.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.common.api.PageResponse;
import com.codehaja.domain.enrollment.dto.CourseEnrollmentDto;
import com.codehaja.domain.enrollment.service.CourseEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<PageResponse<CourseEnrollmentDto.EnrollmentListItem>>> getEnrollments(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CourseEnrollmentDto.EnrollmentListItem> result =
                enrollmentService.getEnrollmentsByCourse(courseId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }
}
