package com.codehaja.domain.coding.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class CodingSubmissionDto {

    @Getter
    @Setter
    public static class SubmitRequest {
        private String sourceCode;
        private String language;
        /** true = Grade (hearts/XP apply), false = Run only */
        private boolean grade = false;
        /** Optional stdin for Run mode */
        private String stdin;
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
        private List<TestCaseResult> testCaseResults;
        // Gamification
        private int xpGained = 0;
        private int currentHearts = 5;
        private LocalDateTime heartsRefillAt;
    }

    @Getter
    @Setter
    public static class TestCaseResult {
        private int index;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private boolean passed;
        private String status; // PASSED, FAILED, ERROR
    }
}