package com.codehaja.domain.section.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import com.codehaja.domain.course.entity.Course;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "course_sections")
@Getter
@Setter
public class CourseSection extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "course_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    private Integer hours;

    private Integer points;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}