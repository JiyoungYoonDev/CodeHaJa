package com.codehaja.domain.project.repository;

import com.codehaja.domain.project.entity.ProjectSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectSubmissionRepository extends JpaRepository<ProjectSubmission, Long> {

    Optional<ProjectSubmission> findTopByLectureItemIdAndUserEmailOrderByCreatedAtDesc(
            Long lectureItemId, String email);

    boolean existsByLectureItemIdAndUserEmail(Long lectureItemId, String email);
}
