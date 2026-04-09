package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_generation_jobs", indexes = {
        @Index(name = "idx_gen_job_course", columnList = "course_id"),
        @Index(name = "idx_gen_job_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class CourseGenerationJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "topic", length = 500)
    private String topic;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "structured_output_used")
    private Boolean structuredOutputUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GenerationJobStatus status;

    @Column(name = "total_lectures")
    private Integer totalLectures;

    @Column(name = "completed_lectures")
    private Integer completedLectures;

    @Column(name = "failed_lectures")
    private Integer failedLectures;

    @Column(name = "total_prompt_tokens")
    private Integer totalPromptTokens;

    @Column(name = "total_completion_tokens")
    private Integer totalCompletionTokens;

    @Column(name = "total_cost_usd", precision = 10, scale = 6)
    private BigDecimal totalCostUsd;

    @Column(name = "total_latency_ms")
    private Long totalLatencyMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_output_id")
    private GenerationOutput structureOutput;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
