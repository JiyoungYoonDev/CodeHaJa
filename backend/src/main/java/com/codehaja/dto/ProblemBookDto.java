package com.codehaja.dto;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

public class ProblemBookDto {
    @Getter
    @Setter
    public static class CreateRequest {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        private String title;
        private String bookDescription;
        private String courseCategory;
        private String difficulty;
        private double rating;
        private Integer projectsCount;
        private Integer hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private String status;

        private JsonNode detailedCurriculum;

        @JsonProperty("course_sections")
        private List<CourseSectionRequest> courseSections;
        private List<String> problemKeywords;

        @JsonProperty("detailedCurriculum")
        public void setDetailedCurriculum(Object value) {
            this.detailedCurriculum = value == null ? null : MAPPER.valueToTree(value);
        }

        public JsonNode getDetailedCurriculum() {
            return detailedCurriculum;
        }
    }

    @Getter
    @Setter
    public static class CourseSectionRequest {
        private Long id;
        private String title;
        private String description;
        private Integer subCount;
        private Integer hours;
        private Integer points;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private String title;
        private String bookDescription;
        private String courseCategory;
        private String difficulty;
        private double rating;
        private Integer projectsCount;
        private double hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private String status;
        private JsonNode detailedCurriculum;

        @JsonProperty("course_sections")
        private List<CourseSectionResponse> courseSections;
    }

    @Getter
    @Setter
    public static class CourseSectionResponse {
        private Long id;
        private String title;
        private String description;
        private Integer subCount;
        private Integer hours;
        private Integer points;
    }
}
