package com.codehaja.domain.quiz.repository;

import com.codehaja.domain.quiz.entity.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    Optional<QuizSubmission> findTopByLectureItemIdAndUserEmailOrderByCreatedAtDesc(
            Long lectureItemId, String email);

    Optional<QuizSubmission> findByLectureItemIdAndUserId(Long lectureItemId, Long userId);

    boolean existsByLectureItemIdAndUserEmail(Long lectureItemId, String email);
}
