package com.codehaja.domain.progress.repository;

import com.codehaja.domain.progress.entity.LectureItemProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LectureItemProgressRepository extends JpaRepository<LectureItemProgress, Long> {

    Optional<LectureItemProgress> findByUserIdAndLectureItemId(Long userId, Long lectureItemId);

    long countByUserIdAndCompletedAtIsNotNullAndLectureItem_Lecture_CourseSection_CourseId(Long userId, Long courseId);

    @Query("SELECT p.lectureItem.id FROM LectureItemProgress p WHERE p.user.id = :userId AND p.completedAt IS NOT NULL AND p.lectureItem.lecture.courseSection.course.id = :courseId")
    List<Long> findCompletedItemIdsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
