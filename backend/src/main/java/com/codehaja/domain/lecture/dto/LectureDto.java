package com.codehaja.domain.lecture.dto;

import com.codehaja.domain.lecture.entity.LectureType;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Getter;
import lombok.Setter;

public class LectureDto {

    @Getter
    @Setter
    public static class CreateRequest {
        private String title;
        private String description;
        private String contentJson;
        private Integer sortOrder;
        private Integer durationMinutes;
        private Boolean isPreview;
        private Boolean isPublished;
        private LectureType lectureType;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String title;
        private String description;
        private String contentJson;
        private Integer sortOrder;
        private Integer durationMinutes;
        private Boolean isPreview;
        private Boolean isPublished;
        private LectureType lectureType;
    }

    @Getter
    @Setter
    public static class SummaryResponse {
        private Long id;
        private Long courseSectionId;
        private String courseSectionTitle;
        private String title;
        private String description;
        private Integer sortOrder;
        private Integer durationMinutes;
        private Boolean isPreview;
        private Boolean isPublished;
        private LectureType lectureType;
        private Long itemCount;
        private Long firstItemId;
    }

    @Getter
    @Setter
    public static class DetailResponse {
        private Long id;
        private Long courseSectionId;
        private String courseSectionTitle;
        private Long courseId;
        private String courseTitle;
        private String title;
        private String description;
        @JsonRawValue
        private String contentJson;
        private Integer sortOrder;
        private Integer durationMinutes;
        private Boolean isPreview;
        private Boolean isPublished;
        private LectureType lectureType;
        private Long itemCount;
    }

    @Getter
    @Setter
    public static class ReorderRequest {
        private Long id;
        private Integer sortOrder;
    }

    @Getter
    @Setter
    public static class PreviewRequest {
        private Boolean isPreview;
    }

    @Getter
    @Setter
    public static class PublishRequest {
        private Boolean isPublished;
    }
}