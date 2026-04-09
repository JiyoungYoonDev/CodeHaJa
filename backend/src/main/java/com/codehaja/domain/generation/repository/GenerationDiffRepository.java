package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.GenerationDiff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerationDiffRepository extends JpaRepository<GenerationDiff, Long> {

    List<GenerationDiff> findByJobIdOrderByCreatedAtDesc(Long jobId);

    long countByJobId(Long jobId);
}
