package com.codehaja.domain.checkpoint.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class CheckpointSubmissionDto {

    @Getter
    @Setter
    public static class SubmitRequest {
        private String blockId;
        private String userAnswer;
        private String correctAnswer;
        private boolean correct;
    }

    @Getter
    @Setter
    @Builder
    public static class Response {
        private Long id;
        private Long lectureItemId;
        private String blockId;
        private String userAnswer;
        private String correctAnswer;
        private boolean correct;
        private LocalDateTime createdAt;
        private Integer currentHearts;
        private LocalDateTime heartsRefillAt;
    }

    @Getter
    @Builder
    public static class ItemSubmissions {
        private Long lectureItemId;
        private List<Response> submissions;
    }
}
