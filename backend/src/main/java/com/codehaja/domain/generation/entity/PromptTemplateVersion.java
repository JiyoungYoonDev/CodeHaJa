package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Future: Versioned prompt content linked to a PromptTemplate.
 */
@Entity
@Table(name = "prompt_template_versions", indexes = {
        @Index(name = "idx_ptv_template", columnList = "template_id"),
        @Index(name = "idx_ptv_active", columnList = "template_id, is_active")
})
@Getter
@Setter
@NoArgsConstructor
public class PromptTemplateVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PromptTemplate template;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables; // JSON array of variable placeholders

    @Column(name = "change_notes", columnDefinition = "TEXT")
    private String changeNotes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
