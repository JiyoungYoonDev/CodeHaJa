package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Future: Stores diff between AI-generated output and final edited result.
 */
@Entity
@Table(name = "generation_diffs", indexes = {
        @Index(name = "idx_gd_job", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
public class GenerationDiff extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private CourseGenerationJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "diff_type", nullable = false, length = 20)
    private DiffType diffType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "before_output_id")
    private GenerationOutput beforeOutput;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "after_output_id")
    private GenerationOutput afterOutput;

    @Column(name = "diff_json", columnDefinition = "TEXT")
    private String diffJson;
}
