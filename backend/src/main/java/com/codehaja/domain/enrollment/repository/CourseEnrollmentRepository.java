package com.codehaja.domain.enrollment.repository;

import com.codehaja.domain.enrollment.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    @EntityGraph(attributePaths = {"user"})
    org.springframework.data.domain.Page<CourseEnrollment> findByCourseId(Long courseId, org.springframework.data.domain.Pageable pageable);

    long countByCourseId(Long courseId);

    long countByUserId(Long userId);
}
