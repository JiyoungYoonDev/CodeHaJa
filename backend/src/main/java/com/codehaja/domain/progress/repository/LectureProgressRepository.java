package com.codehaja.domain.progress.repository;

import com.codehaja.domain.progress.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {
    Optional<LectureProgress> findByAnonymousUserIdAndLectureId(Long anonymousUserId, Long lectureId);
}