package com.codehaja.domain.progress.repository;

import com.codehaja.domain.progress.entity.LectureItemEntryProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureItemEntryProgressRepository extends JpaRepository<LectureItemEntryProgress, Long> {
    Optional<LectureItemEntryProgress> findByUserIdAndLectureItemEntryId(Long userId, Long lectureItemEntryId);
}