package com.codehaja.domain.lectureitementry.dto;

import com.codehaja.domain.lectureitementry.entity.AccessLevel;
import com.codehaja.domain.lectureitementry.entity.LectureItemEntryType;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.databind.JsonNode;

public class LectureItemEntryDto {

    @Getter
    @Setter
    public static class CreateRequest {
        private String title;
        private LectureItemEntryType entryType;
        private String prompt;
        private JsonNode contentJson;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
        private Boolean isActive;
        private AccessLevel accessLevel;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String title;
        private LectureItemEntryType entryType;
        private String prompt;
        private JsonNode contentJson;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
        private Boolean isActive;
        private AccessLevel accessLevel;
    }

    @Getter
    @Setter
    public static class SummaryResponse {
        private Long id;
        private Long lectureItemId;
        private String lectureItemTitle;
        private Long lectureId;
        private String lectureTitle;
        private String title;
        private LectureItemEntryType entryType;
        private String prompt;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
        private Boolean isActive;
        private AccessLevel accessLevel;
    }

    @Getter
    @Setter
    public static class DetailResponse {
        private Long id;
        private Long lectureItemId;
        private String lectureItemTitle;
        private Long lectureId;
        private String lectureTitle;
        private Long courseSectionId;
        private String courseSectionTitle;
        private Long courseId;
        private String courseTitle;
        private String title;
        private LectureItemEntryType entryType;
        private String prompt;
        private JsonNode contentJson;
        private Integer sortOrder;
        private Integer points;
        private Boolean isRequired;
        private Boolean isActive;
        private AccessLevel accessLevel;
    }

    @Getter
    @Setter
    public static class ReorderRequest {
        private Long id;
        private Integer sortOrder;
    }
}