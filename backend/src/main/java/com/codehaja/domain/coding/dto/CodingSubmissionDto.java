package com.codehaja.domain.coding.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Getter;
import lombok.Setter;

public class CodingSubmissionDto {

    @Getter
    @Setter
    public static class SubmitRequest {
        private String sourceCode;
        private String language;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private Long lectureItemId;
        private String sourceCode;
        private String language;
        private String submissionStatus;
        private Integer passedCount;
        private Integer totalCount;
        private Long executionTimeMs;
        private String stdout;
        private String stderr;
        @JsonRawValue
        private String resultJson;
    }
}