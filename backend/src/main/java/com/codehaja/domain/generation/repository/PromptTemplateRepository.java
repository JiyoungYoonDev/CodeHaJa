package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.GenerationTaskType;
import com.codehaja.domain.generation.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    Optional<PromptTemplate> findByName(String name);

    boolean existsByName(String name);

    List<PromptTemplate> findByTaskType(GenerationTaskType taskType);
}
