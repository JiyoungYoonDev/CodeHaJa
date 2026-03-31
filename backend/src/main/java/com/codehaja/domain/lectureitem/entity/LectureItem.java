package com.codehaja.domain.lectureitem.entity;

import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lecture.entity.Lecture;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "lecture_items")
@Getter
@Setter
public class LectureItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "lecture_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Lecture lecture;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private LectureItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Column(length = 2000)
    private String description;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "content_json", columnDefinition = "TEXT")
    private JsonNode contentJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private Integer points;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;
}