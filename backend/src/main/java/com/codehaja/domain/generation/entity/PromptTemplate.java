package com.codehaja.domain.generation.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Future: Logical prompt template identity.
 * e.g. COURSE_OUTLINE_SYSTEM, LECTURE_CONTENT_USER
 */
@Entity
@Table(name = "prompt_templates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_prompt_template_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
public class PromptTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 50)
    private GenerationTaskType taskType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromptTemplateStatus status = PromptTemplateStatus.DRAFT;
}
