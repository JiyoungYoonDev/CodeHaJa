package com.codehaja.domain.progress.dto;

import com.codehaja.domain.progress.entity.ProgressStatus;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Getter;
import lombok.Setter;

public class LectureItemEntryProgressDto {

    @Getter
    @Setter
    public static class SaveRequest {
        private ProgressStatus status;
        private Boolean isCorrect;
        private Integer score;
        private String answerJson;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private Long lectureItemEntryId;
        private ProgressStatus status;
        private Boolean isCorrect;
        private Integer score;
        @JsonRawValue
        private String answerJson;
    }
}