package com.codehaja.domain.lecture.entity;

import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.section.entity.CourseSection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "lectures")
@Getter
@Setter
public class Lecture extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "course_section_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private CourseSection courseSection;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "content_json", columnDefinition = "TEXT")
    private JsonNode contentJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_preview", nullable = false)
    private Boolean isPreview;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    @Enumerated(EnumType.STRING)
    @Column(name = "lecture_type", nullable = false, length = 30)
    private LectureType lectureType;
}