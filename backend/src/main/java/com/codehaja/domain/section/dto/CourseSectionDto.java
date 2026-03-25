package com.codehaja.domain.section.dto;

import com.codehaja.domain.lecture.dto.LectureDto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

public class CourseSectionDto {

    @Getter
    @Setter
    public static class CreateRequest {
        private String title;
        private String description;
        private Integer hours;
        private Integer points;
        private Integer sortOrder;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private Long id;
        private String title;
        private String description;
        private Integer hours;
        private Integer points;
        private Integer sortOrder;
    }

    @Getter
    @Setter
    public static class SummaryResponse {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private String title;
        private String description;
        private Integer hours;
        private Integer points;
        private Integer sortOrder;
        private Long lectureCount;
        private List<LectureDto.SummaryResponse> lectures;
    }

    @Getter
    @Setter
    public static class DetailResponse {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private String title;
        private String description;
        private Integer hours;
        private Integer points;
        private Integer sortOrder;
        private Long lectureCount;
    }

    @Getter
    @Setter
    public static class ReorderRequest {
        private Long id;
        private Integer sortOrder;
    }
}