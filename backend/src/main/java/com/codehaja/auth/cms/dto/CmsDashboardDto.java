package com.codehaja.auth.cms.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class CmsDashboardDto {

    @Getter
    @Builder
    public static class StatsResponse {
        private long totalCourses;
        private long publishedCourses;
        private long draftCourses;
        private long archivedCourses;
        private long totalUsers;
        private long totalEnrollments;
        private long totalCategories;
        private long totalSections;
        private long totalLectures;
        private long totalLectureItems;
        private long draftItems;
        private long inReviewItems;
        private long publishedItems;
        private List<RecentCourse> recentCourses;
    }

    @Getter
    @Builder
    public static class RecentCourse {
        private Long id;
        private String title;
        private String status;
        private String difficulty;
        private String category;
        private LocalDateTime createdAt;
    }
}
