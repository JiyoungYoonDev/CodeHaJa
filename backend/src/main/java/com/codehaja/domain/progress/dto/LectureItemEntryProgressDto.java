package com.codehaja.domain.progress.dto;

import com.codehaja.domain.progress.entity.ProgressStatus;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

public class LectureItemEntryProgressDto {

    @Getter
    @Setter
    public static class SaveRequest {
        private String anonymousUserKey;
        private ProgressStatus status;
        private Boolean isCorrect;
        private Integer score;
        private JsonNode answerJson;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private Long lectureItemEntryId;
        private ProgressStatus status;
        private Boolean isCorrect;
        private Integer score;
        private JsonNode answerJson;
    }
}