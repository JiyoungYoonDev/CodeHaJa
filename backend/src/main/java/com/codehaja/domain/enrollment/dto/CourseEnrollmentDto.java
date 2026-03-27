package com.codehaja.domain.enrollment.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class CourseEnrollmentDto {

    @Getter @Setter
    public static class EnrollRequest {
        private Long courseId;
    }

    @Getter @Setter
    public static class StatusResponse {
        private Long courseId;
        private boolean enrolled;
        private LocalDateTime enrolledAt;
    }
}
