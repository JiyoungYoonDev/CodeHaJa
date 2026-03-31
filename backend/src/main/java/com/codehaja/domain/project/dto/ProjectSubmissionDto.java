package com.codehaja.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class ProjectSubmissionDto {

    @Getter
    @Setter
    public static class CreateRequest {
        private String submissionData; // JSON string of form fields or editor files
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long lectureItemId;
        @JsonRawValue
        private String submissionData;
        private LocalDateTime createdAt;
    }
}
