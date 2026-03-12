package com.codehaja.domain.lectureitem.dto;

import com.codehaja.domain.lectureitem.entity.LectureItemType;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

public class LectureItemDto {

    @Getter
    @Setter
    public static class CreateRequest {
        private String title;
        private LectureItemType itemType;
        private String description;
        private JsonNode contentJson;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String title;
        private LectureItemType itemType;
        private String description;
        private JsonNode contentJson;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
    }

    @Getter
    @Setter
    public static class SummaryResponse {
        private Long id;
        private Long lectureId;
        private String lectureTitle;
        private String title;
        private LectureItemType itemType;
        private String description;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
        private Long entryCount;
    }

    @Getter
    @Setter
    public static class DetailResponse {
        private Long id;
        private Long lectureId;
        private String lectureTitle;
        private Long courseSectionId;
        private String courseSectionTitle;
        private Long courseId;
        private String courseTitle;
        private String title;
        private LectureItemType itemType;
        private String description;
        private JsonNode contentJson;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
        private Long entryCount;
    }

    @Getter
    @Setter
    public static class ReorderRequest {
        private Long id;
        private Integer sortOrder;
    }
}