package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Future: Stores validation results for a generation output.
 */
@Entity
@Table(name = "validation_results", indexes = {
        @Index(name = "idx_vr_output", columnList = "output_id"),
        @Index(name = "idx_vr_severity", columnList = "severity")
})
@Getter
@Setter
@NoArgsConstructor
public class ValidationResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_id", nullable = false)
    private GenerationOutput output;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private ValidationSeverity severity;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON
}
