package com.codehaja.model;

import com.codehaja.common.config.JsonNodeConverter;

import jakarta.persistence.*;
import tools.jackson.databind.JsonNode;

@Entity
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Lob @Column(columnDefinition = "LONGTEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode description;

    private int difficulty;

    @Column(name = "github_template_url")
    private String githubTemplateUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column
    private boolean isSolved;
}
