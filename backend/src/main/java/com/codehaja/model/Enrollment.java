package com.codehaja.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_book_id")
    private ProblemsBook problemBook;

    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
