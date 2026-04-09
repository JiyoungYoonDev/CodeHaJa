package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.CourseGenerationJob;
import com.codehaja.domain.generation.entity.GenerationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseGenerationJobRepository extends JpaRepository<CourseGenerationJob, Long> {

    Optional<CourseGenerationJob> findByCourseId(Long courseId);

    List<CourseGenerationJob> findByStatus(GenerationJobStatus status);

    List<CourseGenerationJob> findAllByOrderByCreatedAtDesc();
}
