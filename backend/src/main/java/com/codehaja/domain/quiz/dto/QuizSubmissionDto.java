package com.codehaja.domain.quiz.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class QuizSubmissionDto {

    @Getter
    @Setter
    public static class CreateRequest {
        private String answers;     // JSON array of answer objects
        private int totalPoints;
        private int earnedPoints;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long lectureItemId;
        @JsonRawValue
        private String answers;
        private int totalPoints;
        private int earnedPoints;
        private LocalDateTime createdAt;
    }
}
