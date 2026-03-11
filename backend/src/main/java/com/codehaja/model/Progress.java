package com.codehaja.model;

import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "progress")
@Getter
@Setter
public class Progress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problems problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private ProblemsBook book;

    @Column(name = "is_solved", nullable = false)
    private boolean isSolved;

    @Column(name = "remaining_submissions", nullable = false)
    private int remainingSubmissions;

    @Column(name = "total_submissions", nullable = false)
    private int totalSubmissions;

    @Column(name = "solved_at", nullable = true)
    private LocalTime solvedAt;

    @Column(name = "last_submission_time", nullable = true)
    private LocalTime lastSubmissionTime;
}
