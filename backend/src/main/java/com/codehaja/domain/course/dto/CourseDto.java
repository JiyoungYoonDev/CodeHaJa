package com.codehaja.domain.course.dto;

import java.util.List;

import com.codehaja.domain.course.entity.CourseStatus;
import com.codehaja.domain.course.entity.Difficulty;
import com.codehaja.domain.section.dto.CourseSectionDto;

import com.fasterxml.jackson.annotation.JsonRawValue;
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
        private Float rating;
        private Integer projectsCount;
        private Integer hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private CourseStatus status;
        private String detailedCurriculum;
        private List<CourseSectionDto.CreateRequest> sections;
    }

    @Getter
    @Setter
    public static class UpdateRequest {
        private String title;
        private String shortDescription;
        private Long categoryId;
        private Difficulty difficulty;
        private Float rating;
        private Integer projectsCount;
        private Integer hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private CourseStatus status;
        private String detailedCurriculum;
        private List<CourseSectionDto.UpdateRequest> sections;
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
        private Float rating;
        private Integer projectsCount;
        private Integer hours;
        private Integer learnersCount;
        private String badgeType;
        private String provider;
        private String imageUrl;
        private CourseStatus status;
        @JsonRawValue
        private String detailedCurriculum;
        private List<CourseSectionDto.SummaryResponse> sections;
    }

    @Getter
    @Setter
    public static class DetailResponse {
        private Long id;
        private String title;
        private Long categoryId;
        private String categoryName;
        private Float rating;
        private Integer learnersCount;
        private Difficulty difficulty;
        private String description;
        private Integer hours;
        private Integer points;
        private Integer sortOrder;
        private Long lectureCount;
        private Integer totalSections;
        private Integer projectsCount;
        private String imageUrl;
        private CourseStatus status;
        private List<CourseSectionDto.SummaryResponse> sections;
        @JsonRawValue
        private String detailedCurriculum;
    }
}
