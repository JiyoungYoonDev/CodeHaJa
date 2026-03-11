package com.codehaja.model;

import com.codehaja.config.JsonNodeConverter;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "problems_book")
@Getter
@Setter
public class ProblemsBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "problem_title", nullable = false, length = 255)
    private String problemTitle;

    @Column(name = "book_description", nullable = false, columnDefinition = "TEXT")
    private String bookDescription;

    @ManyToOne
    @JoinColumn(name = "course_category_id", nullable = false)
    private CourseCategory courseCategory;

    // @Column(name = "book_category", nullable = false, length = 100)
    // private String bookCategory;

    @Column(name = "book_difficulty", nullable = false)
    private String bookDifficulty;

    @Column(name = "problem_count", nullable = false)
    private long bookCount;

    // @Column(name = "book_image_url", nullable = true, length = 255)
    // private String bookImageUrl;

    @Column(name = "book_projects_count")
    private Integer bookProjectsCount;

    @Column(name = "book_solved_count")
    private long bookSolvedCount;

    @Column(name = "book_submission_count")
    private long bookSubmissionCount;

    @Column(name = "problem_user_joined", nullable = false)
    private boolean bookUserJoined;

    @Column(name = "rating")
    private double rating;
    
    @Column(name="hours")
    private double hours;

    private Integer learnersCount;
    private String badgeType;
    private String provider;
    private String imageUrl;
    private String status;


    @ElementCollection
    @CollectionTable(name = "book_keywords", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "keyword", nullable = true, columnDefinition = "TEXT")
    private List<String> bookKeywords;

    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "TEXT")
    private JsonNode detailedCurriculum;

    @OneToMany(mappedBy = "problemBook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSection> courseSections = new ArrayList<>();
}
