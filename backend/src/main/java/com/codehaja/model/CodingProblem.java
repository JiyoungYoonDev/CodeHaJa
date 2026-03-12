package com.codehaja.model;

import com.codehaja.common.config.JsonNodeConverter;

import jakarta.persistence.*;
import tools.jackson.databind.JsonNode;

@Entity
public class CodingProblem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Lob @Column(columnDefinition = "LONGTEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode description;

    @Column(columnDefinition = "LONGTEXT")
    private String skeletonCode;

    @Column(columnDefinition = "LONGTEXT")
    private String solutionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;
}
