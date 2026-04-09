package com.codehaja.domain.checkpoint.repository;

import com.codehaja.domain.checkpoint.entity.CheckpointSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckpointSubmissionRepository extends JpaRepository<CheckpointSubmission, Long> {

    Optional<CheckpointSubmission> findTopByUserIdAndLectureItemIdAndBlockIdOrderByCreatedAtDesc(
            Long userId, Long lectureItemId, String blockId);

    List<CheckpointSubmission> findByUserIdAndLectureItemIdOrderByCreatedAtDesc(
            Long userId, Long lectureItemId);
}
