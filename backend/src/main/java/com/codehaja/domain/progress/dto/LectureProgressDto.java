package com.codehaja.domain.progress.dto;

import com.codehaja.domain.progress.entity.ProgressStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class LectureProgressDto {

    @Getter
    @Setter
    public static class SaveRequest {
        private ProgressStatus status;
        private Integer currentItemOrder;
        private Integer currentEntryOrder;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private Long lectureId;
        private ProgressStatus status;
        private Integer currentItemOrder;
        private Integer currentEntryOrder;
    }

    @Getter
    @Setter
    public static class CourseSummary {
        private List<Long> completedLectureIds;

        public CourseSummary(List<Long> completedLectureIds) {
            this.completedLectureIds = completedLectureIds;
        }
    }
}