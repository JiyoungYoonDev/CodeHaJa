package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.ValidationResult;
import com.codehaja.domain.generation.entity.ValidationSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationResultRepository extends JpaRepository<ValidationResult, Long> {

    List<ValidationResult> findByOutputIdOrderByIdAsc(Long outputId);

    List<ValidationResult> findByOutputIdAndPassedFalse(Long outputId);

    long countByOutputIdAndSeverity(Long outputId, ValidationSeverity severity);

    long countByOutputIdAndPassedFalse(Long outputId);
}
