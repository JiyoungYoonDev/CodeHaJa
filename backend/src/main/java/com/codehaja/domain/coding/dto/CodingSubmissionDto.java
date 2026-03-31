package com.codehaja.domain.coding.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class CodingSubmissionDto {

    @Getter
    @Setter
    public static class SubmitRequest {
        private String sourceCode;
        private String language;
        /** true = Grade (hearts/XP apply), false = Run only */
        private boolean grade = false;
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
        // Gamification
        private int xpGained = 0;
        private int currentHearts = 5;
        private LocalDateTime heartsRefillAt;
    }
}