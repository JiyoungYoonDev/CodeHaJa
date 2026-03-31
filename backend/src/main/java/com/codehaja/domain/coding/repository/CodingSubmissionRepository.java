package com.codehaja.domain.coding.repository;

import com.codehaja.domain.coding.entity.CodingSubmission;
import com.codehaja.domain.coding.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {
    List<CodingSubmission> findAllByUserIdAndLectureItemIdOrderByCreatedAtDesc(Long userId, Long lectureItemId);

    boolean existsByUserIdAndLectureItemIdAndSubmissionStatus(Long userId, Long lectureItemId, SubmissionStatus status);

    @Query("SELECT DISTINCT cs.lectureItem.id FROM CodingSubmission cs " +
           "WHERE cs.user.id = :userId AND cs.submissionStatus = :status " +
           "AND cs.lectureItem.lecture.courseSection.course.id = :courseId")
    List<Long> findDistinctLectureItemIdsByUserIdAndStatusAndCourseId(
            @Param("userId") Long userId,
            @Param("status") SubmissionStatus status,
            @Param("courseId") Long courseId);
}