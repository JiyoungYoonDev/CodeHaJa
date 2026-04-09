package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lecture_content_batches", indexes = {
        @Index(name = "idx_lcb_task", columnList = "task_id"),
        @Index(name = "idx_lcb_status", columnList = "status"),
        @Index(name = "idx_lcb_parent", columnList = "parent_batch_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LectureContentBatch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private LectureContentTask task;

    @Column(name = "batch_index", nullable = false)
    private Integer batchIndex;

    /** JSON array of item titles in this batch, e.g. ["Introduction to X", "Worked Examples: X"] */
    @Column(name = "item_titles", columnDefinition = "TEXT", nullable = false)
    private String itemTitles;

    /** Comma-separated item types, e.g. "RICH_TEXT,RICH_TEXT" */
    @Column(name = "item_types", length = 200, nullable = false)
    private String itemTypes;

    @Column(name = "items_in_batch", nullable = false)
    private Integer itemsInBatch;

    @Column(name = "items_matched")
    private Integer itemsMatched;

    /** JSON array of item titles that were successfully matched, e.g. ["Introduction to X"] */
    @Column(name = "matched_item_titles", columnDefinition = "TEXT")
    private String matchedItemTitles;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GenerationTaskStatus status;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_output_tokens", nullable = false)
    private Integer maxOutputTokens;

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

    // ── Split tracking ──

    /** Self-reference to the parent batch that was SPLIT to create this child batch. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_batch_id")
    private LectureContentBatch parentBatch;

    /** true = counts toward aggregation. false = SPLIT parent, record-only. */
    @Column(name = "is_leaf", nullable = false, columnDefinition = "boolean not null default true")
    private Boolean isLeaf = true;

    /** Why this batch was split, e.g. "MAX_TOKENS". Null if not split. */
    @Column(name = "split_reason", length = 50)
    private String splitReason;

    /** AI finish reason: STOP, MAX_TOKENS, SAFETY, etc. Stored directly to avoid lazy-load issues. */
    @Column(name = "finish_reason", length = 30)
    private String finishReason;

    /** Whether the AI response was truncated (MAX_TOKENS). */
    @Column(name = "truncated", columnDefinition = "boolean default false")
    private Boolean truncated = false;
}
