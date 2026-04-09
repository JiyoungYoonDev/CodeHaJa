package com.codehaja.domain.generation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

public class CourseGenerationDto {

    @Getter
    @Setter
    public static class GenerateRequest {
        private String topic;
        private String level;
        private String targetAudience;
        private Integer numberOfSections;
        private String tone;
        private String accessPolicy;
        private Long categoryId;
        private String extraInstructions;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeneratedCourse {
        private String title;
        private String description;
        private String difficulty;
        private List<GeneratedSection> sections;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeneratedSection {
        private String title;
        private String description;
        private Integer hours;
        private Integer points;

        @JsonAlias({"sort_order", "sortorder"})
        private Integer sortOrder;

        private List<GeneratedLecture> lectures;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeneratedLecture {
        private String title;
        private String description;

        @JsonAlias({"lecture_type", "lecturetype"})
        private String lectureType;

        @JsonAlias({"sort_order", "sortorder"})
        private Integer sortOrder;

        @JsonAlias({"duration_minutes", "durationminutes"})
        private Integer durationMinutes;

        @JsonAlias({"lecture_items", "lectureitems", "items"})
        private List<GeneratedLectureItem> lectureItems;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeneratedLectureItem {
        private String title;
        private String description;

        @JsonAlias({"item_type", "itemtype", "type"})
        private String itemType;

        @JsonAlias({"sort_order", "sortorder"})
        private Integer sortOrder;

        private Integer points;

        @JsonAlias({"is_required", "isrequired", "required"})
        private Boolean isRequired;

        @JsonAlias({"access_level", "accesslevel"})
        private String accessLevel;

        private String content;

        @JsonAlias({"external_links", "externallinks"})
        private String externalLinks;
    }

    @Getter
    @Setter
    public static class GenerateResponse {
        private Long courseId;
        private String courseTitle;
        private String status;
        private Integer totalSections;
        private Integer totalLectures;
        private Integer totalLectureItems;
    }

    @Getter
    @Setter
    public static class AddSectionRequest {
        private Long courseId;
        private String sectionTopic;
        private Integer numberOfLectures;
        private String extraInstructions;
    }

    @Getter
    @Setter
    public static class AddLectureRequest {
        private Long sectionId;
        private String lectureTopic;
        private String extraInstructions;
    }

    @Getter
    @Setter
    public static class AddSectionResponse {
        private Long courseId;
        private Long sectionId;
        private String sectionTitle;
        private Integer totalLectures;
        private Integer totalLectureItems;
    }

    @Getter
    @Setter
    public static class AddLectureResponse {
        private Long sectionId;
        private Long lectureId;
        private String lectureTitle;
        private Integer totalLectureItems;
    }

    @Getter
    @Setter
    public static class AddItemRequest {
        private Long lectureId;
        private String itemTopic;
        private String itemType;           // optional: RICH_TEXT, QUIZ_SET, CODING_SET, CHECKPOINT — AI decides if null
        private String extraInstructions;
    }

    @Getter
    @Setter
    public static class AddItemResponse {
        private Long lectureId;
        private Long itemId;
        private String itemTitle;
        private String itemType;
    }
}
