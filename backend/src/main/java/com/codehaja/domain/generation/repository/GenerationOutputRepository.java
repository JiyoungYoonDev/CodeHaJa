package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.GenerationOutput;
import com.codehaja.domain.generation.entity.GenerationTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerationOutputRepository extends JpaRepository<GenerationOutput, Long> {

    List<GenerationOutput> findByJobIdOrderByCreatedAtAsc(Long jobId);

    List<GenerationOutput> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    List<GenerationOutput> findByTaskTypeAndSuccessOrderByCreatedAtDesc(GenerationTaskType taskType, Boolean success);

    // Prompt template version tracking
    List<GenerationOutput> findByPromptTemplateVersionIdOrderByCreatedAtDesc(Long versionId);

    long countByPromptTemplateVersionId(Long versionId);

    long countByPromptTemplateVersionIdAndSuccess(Long versionId, Boolean success);
}
