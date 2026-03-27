package com.codehaja.domain.progress.repository;

import com.codehaja.domain.progress.entity.LectureItemProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureItemProgressRepository extends JpaRepository<LectureItemProgress, Long> {

    Optional<LectureItemProgress> findByUserIdAndLectureItemId(Long userId, Long lectureItemId);

    long countByUserIdAndCompletedAtIsNotNullAndLectureItem_Lecture_CourseSection_CourseId(Long userId, Long courseId);
}
