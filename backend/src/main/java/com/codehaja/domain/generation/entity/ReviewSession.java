package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Future: Human review session for a generation job.
 */
@Entity
@Table(name = "review_sessions", indexes = {
        @Index(name = "idx_rs_job", columnList = "job_id"),
        @Index(name = "idx_rs_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
public class ReviewSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private CourseGenerationJob job;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ReviewAction action;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
