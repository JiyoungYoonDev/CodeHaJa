package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "generation_outputs", indexes = {
        @Index(name = "idx_gen_output_job", columnList = "job_id"),
        @Index(name = "idx_gen_output_task", columnList = "task_id"),
        @Index(name = "idx_gen_output_batch", columnList = "batch_id"),
        @Index(name = "idx_gen_output_task_type", columnList = "task_type"),
        @Index(name = "idx_gen_output_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class GenerationOutput extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private CourseGenerationJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private LectureContentTask task;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 50)
    private GenerationTaskType taskType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "system_prompt_hash", length = 64)
    private String systemPromptHash;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    @Column(name = "raw_output", columnDefinition = "TEXT")
    private String rawOutput;

    @Column(name = "parsed_output", columnDefinition = "TEXT")
    private String parsedOutput;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_strategy", length = 30)
    private OutputParseStrategy parseStrategy;

    @Column(name = "structured_schema_used")
    private Boolean structuredSchemaUsed;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "candidates_tokens")
    private Integer candidatesTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "thinking_tokens")
    private Integer thinkingTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "estimated_cost_usd", precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(name = "finish_reason", length = 30)
    private String finishReason;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "success", nullable = false)
    private Boolean success = false;

    // Future FK — nullable until prompt versioning is implemented
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_version_id")
    private PromptTemplateVersion promptTemplateVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private LectureContentBatch batch;
}
