package com.codehaja.domain.course.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.course.dto.CourseDto;
import com.codehaja.domain.course.entity.CourseStatus;
import com.codehaja.domain.course.entity.Difficulty;
import com.codehaja.domain.course.service.CourseService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
/* 
POST /api/courses
GET /api/courses/{courseId}
GET /api/courses
PUT /api/courses/{courseId}
DELETE /api/courses/{courseId}
PATCH /api/courses/{courseId}/status

*/
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseDto.Response>> createCourse(
            @RequestBody CourseDto.CreateRequest request
    ) {
        CourseDto.Response response = courseService.createCourse(request);
        return ResponseEntity.ok(ApiResponse.ok("Course created successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseDto.Response>>> getCourses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) CourseStatus status
    ) {
        List<CourseDto.Response> response = courseService.getCourses(categoryId, difficulty, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDto.DetailResponse>> getCourse(
            @PathVariable Long courseId
    ) {
        CourseDto.DetailResponse response = courseService.getCourse(courseId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // @GetMapping("/detail/{courseId}")
    // public ResponseEntity<ApiResponse<CourseDto.DetailResponse>> getCourseDetail(
    //         @PathVariable Long courseId
    // ) {
    //     CourseDto.DetailResponse response = courseService.getCourse(courseId);
    //     return ResponseEntity.ok(ApiResponse.ok(response));
    // }

    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDto.Response>> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CourseDto.UpdateRequest request
    ) {
        CourseDto.Response response = courseService.updateCourse(courseId, request);
        return ResponseEntity.ok(ApiResponse.ok("Course updated successfully.", response));
    }

    @PatchMapping("/{courseId}/status")
    public ResponseEntity<ApiResponse<CourseDto.Response>> updateCourseStatus(
            @PathVariable Long courseId,
            @RequestBody CourseStatusRequest request
    ) {
        CourseDto.Response response = courseService.updateCourseStatus(courseId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Course status updated successfully.", response));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable Long courseId
    ) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(ApiResponse.ok("Course deleted successfully."));
    }

    @Getter
    @Setter
    public static class CourseStatusRequest {
        private CourseStatus status;
    }
}
