package com.codehaja.domain.course.entity;

import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.category.entity.CourseCategory;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "courses")
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CourseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Difficulty difficulty;

    private Float rating;

    @Column(name = "projects_count")
    private Integer projectsCount;

    private Integer hours;

    @Column(name = "learners_count")
    private Integer learnersCount;

    @Column(name = "badge_type", length = 50)
    private String badgeType;

    @Column(length = 100)
    private String provider;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseStatus status;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "detailed_curriculum", columnDefinition = "TEXT")
    private JsonNode detailedCurriculum;
}
