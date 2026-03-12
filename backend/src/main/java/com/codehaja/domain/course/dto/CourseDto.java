package com.codehaja.domain.course.dto;

import com.codehaja.domain.course.entity.CourseStatus;
import com.codehaja.domain.course.entity.Difficulty;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

public class CourseDto {
    @Getter
    @Setter
    public static class CreateRequest {
        private String title;
        private String shortDescription;
        private Long categoryId;
        private Difficulty difficulty;
        private Integer rating;
        private Integer projectsCount;
        private Integer hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private CourseStatus status;
        private JsonNode detailedCurriculum;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String title;
        private String shortDescription;
        private Long categoryId;
        private Difficulty difficulty;
        private Integer rating;
        private Integer projectsCount;
        private Integer hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private CourseStatus status;
        private JsonNode detailedCurriculum;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private String title;
        private String shortDescription;
        private Long categoryId;
        private String categoryName;
        private Difficulty difficulty;
        private Integer rating;
        private Integer projectsCount;
        private Integer hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private CourseStatus status;
        private JsonNode detailedCurriculum;
    }
}
