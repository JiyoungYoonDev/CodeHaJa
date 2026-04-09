package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lecture_content_tasks", indexes = {
        @Index(name = "idx_lct_job", columnList = "job_id"),
        @Index(name = "idx_lct_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class LectureContentTask extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private CourseGenerationJob job;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "lecture_title")
    private String lectureTitle;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "section_title")
    private String sectionTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GenerationTaskStatus status;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "items_total")
    private Integer itemsTotal;

    @Column(name = "items_matched")
    private Integer itemsMatched;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_id")
    private GenerationOutput output;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_strategy", length = 30)
    private OutputParseStrategy parseStrategy;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ── Batch tracking fields ──

    @Column(name = "total_batches")
    private Integer totalBatches;

    @Column(name = "completed_batches")
    private Integer completedBatches;

    @Column(name = "failed_batches")
    private Integer failedBatches;

    /** "SINGLE" (legacy) or "BATCHED" */
    @Column(name = "generation_mode", length = 20)
    private String generationMode;
}
