package com.codehaja.domain.progress.repository;

import com.codehaja.domain.progress.entity.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {
    Optional<LectureProgress> findByUserIdAndLectureId(Long userId, Long lectureId);
    List<LectureProgress> findByUserIdAndLecture_CourseSection_CourseId(Long userId, Long courseId);
}