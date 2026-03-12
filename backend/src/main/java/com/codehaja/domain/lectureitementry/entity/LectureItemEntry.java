package com.codehaja.domain.lectureitementry.entity;

import com.codehaja.common.converter.JsonNodeConverter;
import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "lecture_item_entries")
@Getter
@Setter
public class LectureItemEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "lecture_item_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private LectureItem lectureItem;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 50)
    private LectureItemEntryType entryType;

    @Column(length = 4000)
    private String prompt;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "content_json", columnDefinition = "TEXT")
    private JsonNode contentJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private Integer points;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 30)
    private AccessLevel accessLevel;
}