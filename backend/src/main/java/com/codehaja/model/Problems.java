package com.codehaja.model;
import com.codehaja.config.JsonNodeConverter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "problems")
@Getter
@Setter
public class Problems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = true)
    private Lecture lecture;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private ProblemsBook bookId;

    @Column(name = "problem_title", nullable = false, length = 255)
    private String problemTitle;

    @Lob
    @Column(name = "problem_content", columnDefinition = "LONGTEXT", nullable = false)
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode problemContent;

    @Column(name = "problem_answer", nullable = false, columnDefinition = "TEXT")
    private String problemAnswer;

    @Column(name = "problem_hints", nullable = true, columnDefinition = "TEXT")
    private String problemHints;

    @Column(name = "problem_code_skeleton", nullable = true, columnDefinition = "TEXT")
    private String problemCodeSkeleton;

    @Column(name = "difficulty", nullable = false)
    private int difficulty;
}
